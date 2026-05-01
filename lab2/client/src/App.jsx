import { useCallback, useEffect, useMemo, useState } from "react";
import axios from "axios";
import "./App.css";

/* eslint-disable react-hooks/set-state-in-effect */

const API = "http://localhost:3000/api";

const emptyTask = {
  title: "",
  description: "",
  status: "planned",
  priority: "medium",
  deadline: "",
  assignedTo: "",
  reminderEmail: "student@test.com",
};

const statusLabels = {
  planned: "Заплановано",
  in_progress: "В процесі",
  done: "Виконано",
};

const priorityLabels = {
  low: "Низький",
  medium: "Середній",
  high: "Високий",
};

export default function App() {
  const [token, setToken] = useState(localStorage.getItem("token"));
  const [user, setUser] = useState(
    JSON.parse(localStorage.getItem("user") || "null")
  );
  const [users, setUsers] = useState([]);
  const [tasks, setTasks] = useState([]);
  const [selectedTask, setSelectedTask] = useState(null);
  const [analytics, setAnalytics] = useState(null);
  const [report, setReport] = useState(null);
  const [editingTaskId, setEditingTaskId] = useState(null);
  const [message, setMessage] = useState("");

  const [loginForm, setLoginForm] = useState({
    email: "student@test.com",
    password: "user123",
  });
  const [taskForm, setTaskForm] = useState(emptyTask);

  const authHeader = useMemo(
    () => ({
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }),
    [token]
  );

  const loadTasks = useCallback(async () => {
    const res = await axios.get(`${API}/tasks`, authHeader);
    setTasks(res.data);
  }, [authHeader]);

  const loadAnalytics = useCallback(async () => {
    const res = await axios.get(`${API}/analytics`, authHeader);
    setAnalytics(res.data);
  }, [authHeader]);

  const loadReport = useCallback(async () => {
    const res = await axios.get(`${API}/report`, authHeader);
    setReport(res.data);
  }, [authHeader]);

  const loadUsers = useCallback(async () => {
    if (user?.role !== "admin") {
      setUsers([]);
      return;
    }

    const res = await axios.get(`${API}/users`, authHeader);
    setUsers(res.data);
  }, [authHeader, user?.role]);

  const refreshData = useCallback(async () => {
    await Promise.all([loadTasks(), loadAnalytics(), loadReport(), loadUsers()]);
  }, [loadAnalytics, loadReport, loadTasks, loadUsers]);

  async function login(e) {
    e.preventDefault();
    setMessage("");

    try {
      const res = await axios.post(`${API}/login`, loginForm);

      localStorage.setItem("token", res.data.token);
      localStorage.setItem("user", JSON.stringify(res.data.user));

      setToken(res.data.token);
      setUser(res.data.user);
    } catch (error) {
      setMessage(error.response?.data?.message || "Не вдалося увійти");
    }
  }

  function logout() {
    localStorage.clear();
    setToken(null);
    setUser(null);
    setUsers([]);
    setTasks([]);
    setSelectedTask(null);
    setAnalytics(null);
    setReport(null);
    setEditingTaskId(null);
    setTaskForm(emptyTask);
  }

  function resetForm() {
    setEditingTaskId(null);
    setTaskForm({
      ...emptyTask,
      assignedTo: user?.role === "admin" ? users[0]?.id || "" : "",
    });
  }

  async function saveTask(e) {
    e.preventDefault();
    setMessage("");

    const payload = {
      ...taskForm,
      assignedTo: user?.role === "admin" ? taskForm.assignedTo : undefined,
    };

    try {
      if (editingTaskId) {
        await axios.put(`${API}/tasks/${editingTaskId}`, payload, authHeader);
        setMessage("Задачу оновлено");
      } else {
        await axios.post(`${API}/tasks`, payload, authHeader);
        setMessage("Задачу створено");
      }

      resetForm();
      await refreshData();
    } catch (error) {
      setMessage(error.response?.data?.message || "Не вдалося зберегти задачу");
    }
  }

  async function showDetails(id) {
    const res = await axios.get(`${API}/tasks/${id}`, authHeader);
    setSelectedTask(res.data);
  }

  function startEdit(task) {
    setEditingTaskId(task.id);
    setTaskForm({
      title: task.title,
      description: task.description,
      status: task.status,
      priority: task.priority,
      deadline: task.deadline || "",
      assignedTo: task.assignedTo || "",
      reminderEmail: task.reminderEmail || "",
    });
    setSelectedTask(task);
  }

  async function updateTaskStatus(id, status) {
    await axios.put(`${API}/tasks/${id}`, { status }, authHeader);
    await refreshData();
  }

  async function deleteTask(id) {
    await axios.delete(`${API}/tasks/${id}`, authHeader);
    setSelectedTask(null);
    await refreshData();
  }

  async function sendReminder(id) {
    setMessage("");

    try {
      const res = await axios.post(`${API}/tasks/${id}/reminder`, {}, authHeader);
      setMessage(res.data.message || "Email-нагадування сформовано");
      await refreshData();

      if (selectedTask?.id === id) {
        await showDetails(id);
      }
    } catch (error) {
      setMessage(
        error.response?.data?.message ||
          `Не вдалося сформувати нагадування: ${error.message}`
      );
    }
  }

  useEffect(() => {
    if (token) {
      refreshData();
    }
  }, [refreshData, token]);

  useEffect(() => {
    if (!editingTaskId && user?.role === "admin" && users.length > 0) {
      setTaskForm((current) => ({
        ...current,
        assignedTo: current.assignedTo || users[0].id,
      }));
    }
  }, [editingTaskId, user?.role, users]);

  if (!token) {
    return (
      <main className="login-shell">
        <section className="login-panel">
          <div>
            <p className="eyebrow">Лабораторна робота №2</p>
            <h1>Система управління задачами</h1>
            <p className="lead">
              Express API, React SPA, ролі користувачів, планування,
              нагадування та аналітика виконання задач.
            </p>
          </div>

          <form className="auth-form" onSubmit={login}>
            <label>
              Email
              <input
                value={loginForm.email}
                onChange={(e) =>
                  setLoginForm({ ...loginForm, email: e.target.value })
                }
              />
            </label>

            <label>
              Пароль
              <input
                type="password"
                value={loginForm.password}
                onChange={(e) =>
                  setLoginForm({ ...loginForm, password: e.target.value })
                }
              />
            </label>

            <button className="primary-button">Увійти</button>
            {message && <p className="notice error">{message}</p>}
          </form>

          <div className="credentials">
            <span>student@test.com / user123</span>
            <span>admin@test.com / admin123</span>
          </div>
        </section>
      </main>
    );
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <h1>Панель задач</h1>
        </div>
        <div className="user-box">
          <span>{user.name}</span>
          <small>{user.role === "admin" ? "Адміністратор" : "Користувач"}</small>
          <button className="ghost-button" onClick={logout}>
            Вийти
          </button>
        </div>
      </header>

      {message && <p className="notice">{message}</p>}

      {analytics && (
        <section className="metrics-grid">
          <article>
            <span>Усього</span>
            <strong>{analytics.total}</strong>
          </article>
          <article>
            <span>Заплановано</span>
            <strong>{analytics.planned}</strong>
          </article>
          <article>
            <span>В процесі</span>
            <strong>{analytics.inProgress}</strong>
          </article>
          <article>
            <span>Виконано</span>
            <strong>{analytics.completionRate}%</strong>
          </article>
          <article>
            <span>Прострочено</span>
            <strong>{analytics.overdue}</strong>
          </article>
        </section>
      )}

      <section className="workspace">
        <form className="task-form" onSubmit={saveTask}>
          <div className="section-heading">
            <p className="eyebrow">{editingTaskId ? "Редагування" : "Створення"}</p>
            <h2>{editingTaskId ? "Оновити задачу" : "Нова задача"}</h2>
          </div>

          <label>
            Назва
            <input
              value={taskForm.title}
              onChange={(e) =>
                setTaskForm({ ...taskForm, title: e.target.value })
              }
              required
            />
          </label>

          <label>
            Опис
            <textarea
              value={taskForm.description}
              onChange={(e) =>
                setTaskForm({ ...taskForm, description: e.target.value })
              }
              rows="4"
            />
          </label>

          <div className="form-grid">
            <label>
              Статус
              <select
                value={taskForm.status}
                onChange={(e) =>
                  setTaskForm({ ...taskForm, status: e.target.value })
                }
              >
                {Object.entries(statusLabels).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Пріоритет
              <select
                value={taskForm.priority}
                onChange={(e) =>
                  setTaskForm({ ...taskForm, priority: e.target.value })
                }
              >
                {Object.entries(priorityLabels).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="form-grid">
            <label>
              Дедлайн
              <input
                type="date"
                value={taskForm.deadline}
                onChange={(e) =>
                  setTaskForm({ ...taskForm, deadline: e.target.value })
                }
              />
            </label>

            <label>
              Email нагадування
              <input
                type="email"
                value={taskForm.reminderEmail}
                onChange={(e) =>
                  setTaskForm({ ...taskForm, reminderEmail: e.target.value })
                }
              />
            </label>
          </div>

          {user.role === "admin" && (
            <label>
              Виконавець
              <select
                value={taskForm.assignedTo}
                onChange={(e) =>
                  setTaskForm({ ...taskForm, assignedTo: e.target.value })
                }
              >
                {users.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.name} ({item.email})
                  </option>
                ))}
              </select>
            </label>
          )}

          <div className="button-row">
            <button className="primary-button">
              {editingTaskId ? "Зберегти зміни" : "Створити задачу"}
            </button>
            {editingTaskId && (
              <button className="ghost-button" type="button" onClick={resetForm}>
                Скасувати
              </button>
            )}
          </div>
        </form>

        <section className="details-panel">
          <div className="section-heading">
            <p className="eyebrow">Перегляд</p>
            <h2>Деталі задачі</h2>
          </div>
          {selectedTask ? (
            <div className="details-list">
              <p>
                <span>Назва</span>
                {selectedTask.title}
              </p>
              <p>
                <span>Опис</span>
                {selectedTask.description || "Опис не вказано"}
              </p>
              <p>
                <span>Статус</span>
                {statusLabels[selectedTask.status]}
              </p>
              <p>
                <span>Пріоритет</span>
                {priorityLabels[selectedTask.priority]}
              </p>
              <p>
                <span>Дедлайн</span>
                {selectedTask.deadline || "Без дедлайну"}
              </p>
              <p>
                <span>Нагадування</span>
                {selectedTask.reminderSent ? "Надіслано" : "Очікує"}
              </p>
            </div>
          ) : (
            <p className="muted">Оберіть задачу зі списку, щоб побачити деталі.</p>
          )}
        </section>
      </section>

      <section className="task-list">
        <div className="section-heading">
          <p className="eyebrow">CRUD</p>
          <h2>Список задач</h2>
        </div>

        <div className="cards-grid">
          {tasks.map((task) => (
            <article className="task-card" key={task.id}>
              <div className="card-topline">
                <span className={`badge ${task.status}`}>
                  {statusLabels[task.status]}
                </span>
                <span className={`priority ${task.priority}`}>
                  {priorityLabels[task.priority]}
                </span>
              </div>
              <h3>{task.title}</h3>
              <p>{task.description || "Без опису"}</p>
              <small>Дедлайн: {task.deadline || "не встановлено"}</small>

              <div className="card-actions">
                <button onClick={() => showDetails(task.id)}>Деталі</button>
                <button onClick={() => startEdit(task)}>Редагувати</button>
                <button onClick={() => updateTaskStatus(task.id, "in_progress")}>
                  В процесі
                </button>
                <button onClick={() => updateTaskStatus(task.id, "done")}>
                  Виконано
                </button>
                <button onClick={() => sendReminder(task.id)}>Нагадати</button>
                <button className="danger" onClick={() => deleteTask(task.id)}>
                  Видалити
                </button>
              </div>
            </article>
          ))}
        </div>
      </section>

      {report && (
        <section className="report-panel">
          <div className="section-heading">
            <p className="eyebrow">Звітність</p>
            <h2>Звіт виконання</h2>
          </div>
          <p className="lead">{report.summary}</p>

          <div className="report-grid">
            <article>
              <h3>За пріоритетами</h3>
              <p>Низький: {report.byPriority.low}</p>
              <p>Середній: {report.byPriority.medium}</p>
              <p>Високий: {report.byPriority.high}</p>
            </article>

            <article>
              <h3>Найближчі дедлайни</h3>
              {report.upcoming.length ? (
                report.upcoming.map((task) => (
                  <p key={task.id}>
                    {task.deadline}: {task.title}
                  </p>
                ))
              ) : (
                <p>Активних дедлайнів немає</p>
              )}
            </article>

            <article>
              <h3>Прострочені</h3>
              {report.overdue.length ? (
                report.overdue.map((task) => <p key={task.id}>{task.title}</p>)
              ) : (
                <p>Прострочених задач немає</p>
              )}
            </article>
          </div>
        </section>
      )}
    </main>
  );
}
