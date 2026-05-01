const express = require("express");
const cors = require("cors");
const { v4: uuid } = require("uuid");
const jwt = require("jsonwebtoken");
const bcrypt = require("bcryptjs");
const nodemailer = require("nodemailer");
const cron = require("node-cron");
const fs = require("fs");
const path = require("path");

function loadEnvFile() {
  const envPath = path.join(__dirname, ".env");

  if (!fs.existsSync(envPath)) {
    return;
  }

  const lines = fs.readFileSync(envPath, "utf8").split(/\r?\n/);

  for (const line of lines) {
    const trimmed = line.trim();

    if (!trimmed || trimmed.startsWith("#")) {
      continue;
    }

    const separatorIndex = trimmed.indexOf("=");

    if (separatorIndex === -1) {
      continue;
    }

    const key = trimmed.slice(0, separatorIndex).trim();
    const value = trimmed
      .slice(separatorIndex + 1)
      .trim()
      .replace(/^["']|["']$/g, "");

    if (key && process.env[key] === undefined) {
      process.env[key] = value;
    }
  }
}

loadEnvFile();

const app = express();
const PORT = process.env.PORT || 3000;
const SECRET = "lab2-secret-key";
const isEmailDeliveryConfigured = Boolean(process.env.SMTP_HOST);
const mailFrom = process.env.MAIL_FROM || "task-manager@test.com";

app.use(cors());
app.use(express.json());

const users = [
  {
    id: "u1",
    name: "Admin",
    email: "admin@test.com",
    role: "admin",
    password: bcrypt.hashSync("admin123", 8),
  },
  {
    id: "u2",
    name: "Student",
    email: "student@test.com",
    role: "user",
    password: bcrypt.hashSync("user123", 8),
  },
];

let tasks = [
  {
    id: uuid(),
    title: "Підготувати звіт",
    description: "Оформити лабораторну роботу №2",
    status: "planned",
    priority: "high",
    deadline: "2026-05-10",
    assignedTo: "u2",
    reminderEmail: "student@test.com",
    reminderSent: false,
  },
];

function getVisibleTasks(user) {
  return user.role === "admin"
    ? tasks
    : tasks.filter((task) => task.assignedTo === user.id);
}

function canAccessTask(user, task) {
  return user.role === "admin" || task.assignedTo === user.id;
}

function normalizeTaskPayload(body, user, existingTask = {}) {
  const allowedStatuses = ["planned", "in_progress", "done"];
  const allowedPriorities = ["low", "medium", "high"];
  const title = body.title?.trim() ?? existingTask.title;
  const status = body.status ?? existingTask.status ?? "planned";
  const priority = body.priority ?? existingTask.priority ?? "medium";
  const assignedTo =
    user.role === "admin"
      ? body.assignedTo || existingTask.assignedTo || user.id
      : existingTask.assignedTo || user.id;

  if (!title) {
    return { error: "Назва задачі є обов'язковою" };
  }

  if (!allowedStatuses.includes(status)) {
    return { error: "Невідомий статус задачі" };
  }

  if (!allowedPriorities.includes(priority)) {
    return { error: "Невідомий пріоритет задачі" };
  }

  if (!users.some((userItem) => userItem.id === assignedTo)) {
    return { error: "Користувача для призначення не знайдено" };
  }

  return {
    data: {
      title,
      description: body.description?.trim() ?? existingTask.description ?? "",
      status,
      priority,
      deadline: body.deadline || existingTask.deadline || "",
      assignedTo,
      reminderEmail:
        body.reminderEmail?.trim() ?? existingTask.reminderEmail ?? "",
      reminderSent: status === "done" ? Boolean(existingTask.reminderSent) : false,
    },
  };
}

async function sendTaskReminder(task, recipientEmail) {
  if (!transporter) {
    throw new Error(
      "SMTP не налаштовано. Створіть server/.env і вкажіть SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS та MAIL_FROM."
    );
  }

  const info = await transporter.sendMail({
    from: mailFrom,
    to: recipientEmail,
    subject: "Нагадування про задачу",
    text: `Задача "${task.title}" має дедлайн ${task.deadline || "без дати"}.`,
    html: `<p>Задача <b>${task.title}</b> має дедлайн ${task.deadline || "без дати"}.</p>`,
  });

  task.reminderSent = true;
  task.reminderEmail = recipientEmail;
  return info;
}

function auth(req, res, next) {
  const header = req.headers.authorization;

  if (!header) {
    return res.status(401).json({ message: "Немає токена" });
  }

  try {
    const token = header.split(" ")[1];
    req.user = jwt.verify(token, SECRET);
    next();
  } catch {
    res.status(401).json({ message: "Невірний токен" });
  }
}

function adminOnly(req, res, next) {
  if (req.user.role !== "admin") {
    return res.status(403).json({ message: "Доступ лише для адміністратора" });
  }

  next();
}

app.post("/api/login", (req, res) => {
  const { email, password } = req.body;

  const user = users.find((u) => u.email === email);

  if (!user || !bcrypt.compareSync(password, user.password)) {
    return res.status(400).json({ message: "Невірний email або пароль" });
  }

  const token = jwt.sign(
    { id: user.id, name: user.name, role: user.role },
    SECRET,
    { expiresIn: "2h" }
  );

  res.json({
    token,
    user: {
      id: user.id,
      name: user.name,
      role: user.role,
    },
  });
});

app.get("/api/users", auth, adminOnly, (req, res) => {
  res.json(users.map(({ password, ...user }) => user));
});

app.get("/api/tasks", auth, (req, res) => {
  res.json(getVisibleTasks(req.user));
});

app.get("/api/tasks/:id", auth, (req, res) => {
  const task = tasks.find((t) => t.id === req.params.id);

  if (!task) {
    return res.status(404).json({ message: "Задачу не знайдено" });
  }

  if (!canAccessTask(req.user, task)) {
    return res.status(403).json({ message: "Немає доступу" });
  }

  res.json(task);
});

app.post("/api/tasks", auth, (req, res) => {
  const payload = normalizeTaskPayload(req.body, req.user);

  if (payload.error) {
    return res.status(400).json({ message: payload.error });
  }

  const task = {
    id: uuid(),
    ...payload.data,
  };

  tasks.push(task);
  res.status(201).json(task);
});

app.put("/api/tasks/:id", auth, (req, res) => {
  const index = tasks.findIndex((t) => t.id === req.params.id);

  if (index === -1) {
    return res.status(404).json({ message: "Задачу не знайдено" });
  }

  if (!canAccessTask(req.user, tasks[index])) {
    return res.status(403).json({ message: "Немає доступу" });
  }

  const payload = normalizeTaskPayload(req.body, req.user, tasks[index]);

  if (payload.error) {
    return res.status(400).json({ message: payload.error });
  }

  tasks[index] = {
    ...tasks[index],
    ...payload.data,
  };

  res.json(tasks[index]);
});

app.delete("/api/tasks/:id", auth, (req, res) => {
  const task = tasks.find((t) => t.id === req.params.id);

  if (!task) {
    return res.status(404).json({ message: "Задачу не знайдено" });
  }

  if (!canAccessTask(req.user, task)) {
    return res.status(403).json({ message: "Немає доступу" });
  }

  tasks = tasks.filter((t) => t.id !== req.params.id);
  res.json({ message: "Задачу видалено" });
});

app.get("/api/analytics", auth, (req, res) => {
  const visibleTasks = getVisibleTasks(req.user);

  const total = visibleTasks.length;
  const done = visibleTasks.filter((t) => t.status === "done").length;
  const planned = visibleTasks.filter((t) => t.status === "planned").length;
  const inProgress = visibleTasks.filter((t) => t.status === "in_progress").length;
  const overdue = visibleTasks.filter(
    (t) =>
      t.deadline &&
      t.deadline < new Date().toISOString().slice(0, 10) &&
      t.status !== "done"
  ).length;

  res.json({
    total,
    done,
    planned,
    inProgress,
    overdue,
    completionRate: total === 0 ? 0 : Math.round((done / total) * 100),
  });
});

app.get("/api/report", auth, (req, res) => {
  const visibleTasks = getVisibleTasks(req.user);
  const today = new Date().toISOString().slice(0, 10);
  const byPriority = {
    low: visibleTasks.filter((task) => task.priority === "low").length,
    medium: visibleTasks.filter((task) => task.priority === "medium").length,
    high: visibleTasks.filter((task) => task.priority === "high").length,
  };
  const overdue = visibleTasks.filter(
    (task) => task.deadline && task.deadline < today && task.status !== "done"
  );
  const upcoming = visibleTasks
    .filter(
      (task) => task.deadline && task.deadline >= today && task.status !== "done"
    )
    .sort((a, b) => a.deadline.localeCompare(b.deadline))
    .slice(0, 5);

  res.json({
    generatedAt: new Date().toISOString(),
    byPriority,
    overdue,
    upcoming,
    summary: `Звіт містить ${visibleTasks.length} задач, прострочено: ${overdue.length}.`,
  });
});

app.post("/api/tasks/:id/reminder", auth, async (req, res) => {
  const task = tasks.find((t) => t.id === req.params.id);

  if (!task) {
    return res.status(404).json({ message: "Задачу не знайдено" });
  }

  if (!canAccessTask(req.user, task)) {
    return res.status(403).json({ message: "Немає доступу" });
  }

  const assignedUser = users.find((user) => user.id === task.assignedTo);
  const recipientEmail = task.reminderEmail || assignedUser?.email;

  if (!recipientEmail) {
    return res.status(400).json({ message: "Для задачі не вказано email" });
  }

  try {
    const info = await sendTaskReminder(task, recipientEmail);
    res.json({
      message: `Email-нагадування надіслано на ${recipientEmail}`,
      preview: info.message,
    });
  } catch (error) {
    res.status(500).json({
      message: `Не вдалося сформувати нагадування: ${error.message}`,
    });
  }
});

const transporter = isEmailDeliveryConfigured
  ? nodemailer.createTransport({
      host: process.env.SMTP_HOST,
      port: Number(process.env.SMTP_PORT || 587),
      secure: process.env.SMTP_SECURE === "true",
      auth:
        process.env.SMTP_USER && process.env.SMTP_PASS
          ? {
              user: process.env.SMTP_USER,
              pass: process.env.SMTP_PASS,
            }
          : undefined,
    })
  : null;

cron.schedule("* * * * *", async () => {
  const today = new Date().toISOString().slice(0, 10);

  for (const task of tasks) {
    if (
      task.deadline &&
      task.deadline <= today &&
      task.status !== "done" &&
      task.reminderEmail &&
      !task.reminderSent
    ) {
      const assignedUser = users.find((user) => user.id === task.assignedTo);
      const info = await sendTaskReminder(
        task,
        task.reminderEmail || assignedUser?.email
      );
      console.log("Email-нагадування надіслано для:", task.reminderEmail);
    }
  }
});

app.listen(PORT, () => {
  console.log(`Server started on http://localhost:${PORT}`);
});
