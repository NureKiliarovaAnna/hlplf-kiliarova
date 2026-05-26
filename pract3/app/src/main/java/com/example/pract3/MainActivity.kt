package com.example.pract3

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.io.File
import java.time.DateTimeException
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Pract3App()
        }
    }
}

// Модель книжки для бібліотеки.
data class Book(
    val title: String,
    val author: String,
    val totalCount: Int,
    val availableCount: Int
)

// Модель читача. За умовою список взятих книжок зберігається у MutableList.
data class Reader(
    val name: String,
    val borrowedBooks: MutableList<String> = mutableListOf()
)

enum class AppSection(val title: String) {
    WeekDay("День тижня"),
    Birthday("День народження"),
    Files("Файли"),
    Library("Бібліотека")
}

@Composable
fun Pract3App() {
    var selectedSection by remember { mutableStateOf(AppSection.WeekDay) }
    val selectedIndex = AppSection.entries.indexOf(selectedSection)

    MaterialTheme {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(title = { Text("Практична робота №3") })
                    // ScrollableTabRow не обрізає кнопки: вкладки можна прокручувати горизонтально.
                    ScrollableTabRow(selectedTabIndex = selectedIndex) {
                        AppSection.entries.forEachIndexed { index, section ->
                            Tab(
                                selected = selectedIndex == index,
                                onClick = { selectedSection = section },
                                text = { Text(section.title) }
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedSection) {
                        AppSection.WeekDay -> WeekDayScreen()
                        AppSection.Birthday -> BirthdayScreen()
                        AppSection.Files -> FileCounterScreen()
                        AppSection.Library -> LibraryScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
fun WeekDayScreen() {
    var numberText by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }

    SectionCard(title = "Рівень 1. День тижня за номером") {
        Text(
            text = "Відповідність:\n" +
                "1 - Понеділок\n" +
                "2 - Вівторок\n" +
                "3 - Середа\n" +
                "4 - Четвер\n" +
                "5 - П'ятниця\n" +
                "6 - Субота\n" +
                "7 - Неділя"
        )
        OutlinedTextField(
            value = numberText,
            onValueChange = { numberText = it },
            label = { Text("Число від 1 до 7") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            val text = numberText.trim()
            val number = text.toIntOrNull()

            // when використовується для вибору дня тижня за номером.
            result = when {
                text.isEmpty() -> "Помилка: поле не може бути порожнім."
                number == null -> "Помилка: введіть ціле число."
                number !in 1..7 -> "Помилка: число має бути від 1 до 7."
                number == 1 -> "Понеділок"
                number == 2 -> "Вівторок"
                number == 3 -> "Середа"
                number == 4 -> "Четвер"
                number == 5 -> "П'ятниця"
                number == 6 -> "Субота"
                else -> "Неділя"
            }
        }) {
            Text("Показати день")
        }
        ResultText(result)
    }
}

@Composable
fun BirthdayScreen() {
    var name by remember { mutableStateOf("") }
    var dayText by remember { mutableStateOf("") }
    var monthText by remember { mutableStateOf("") }
    var yearText by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }

    SectionCard(title = "Рівень 2. Привітання з днем народження") {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Ім'я") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = dayText,
            onValueChange = { dayText = it },
            label = { Text("День") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = monthText,
            onValueChange = { monthText = it },
            label = { Text("Місяць") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = yearText,
            onValueChange = { yearText = it },
            label = { Text("Рік народження") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            result = checkBirthday(name, dayText, monthText, yearText)
        }) {
            Text("Перевірити")
        }
        ResultText(result)
    }
}

fun checkBirthday(name: String, dayText: String, monthText: String, yearText: String): String {
    val clearName = name.trim()
    val day = dayText.trim().toIntOrNull()
    val month = monthText.trim().toIntOrNull()
    val year = yearText.trim().toIntOrNull()
    val today = LocalDate.now()

    if (clearName.isEmpty()) return "Помилка: введіть ім'я."
    if (day == null || month == null || year == null) {
        return "Помилка: день, місяць і рік мають бути числами."
    }
    if (year < 1900 || year > today.year) {
        return "Помилка: введіть коректний рік народження."
    }

    val birthday = try {
        LocalDate.of(year, month, day)
    } catch (error: DateTimeException) {
        return "Помилка: такої дати народження не існує."
    }

    if (birthday.isAfter(today)) {
        return "Помилка: дата народження не може бути в майбутньому."
    }

    val birthdayThisYear = birthday.withYear(today.year)
    val nextBirthday = if (birthdayThisYear.isBefore(today)) {
        birthdayThisYear.plusYears(1)
    } else {
        birthdayThisYear
    }

    return if (today.monthValue == birthday.monthValue && today.dayOfMonth == birthday.dayOfMonth) {
        val age = Period.between(birthday, today).years
        "Вітаємо, $clearName, з днем народження! Вам виповнюється $age років."
    } else {
        val daysLeft = ChronoUnit.DAYS.between(today, nextBirthday)
        "До дня народження залишилось $daysLeft днів."
    }
}

@Composable
fun FileCounterScreen() {
    val context = LocalContext.current
    var selectedDirectoryUri by remember { mutableStateOf<Uri?>(null) }
    var result by remember { mutableStateOf("") }
    val directoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) {
            result = "Каталог не вибрано."
        } else {
            selectedDirectoryUri = uri
            result = try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                countFilesInDocumentTree(context, uri)
            } catch (error: SecurityException) {
                "Немає доступу до вибраного каталогу: ${error.message ?: "дозвіл не надано"}."
            } catch (error: Exception) {
                "Помилка під час відкриття каталогу: ${error.message ?: "невідома помилка"}."
            }
        }
    }

    SectionCard(title = "Рівень 3. Підрахунок файлів у каталозі") {
        Text(
            text = "Android обмежує доступ до багатьох системних папок. " +
                "Основний спосіб для цього завдання - вибір папки через системний провідник."
        )
        Button(onClick = {
            directoryLauncher.launch(null)
        }) {
            Text("Обрати каталог")
        }
        Button(onClick = {
            val uri = selectedDirectoryUri
            result = if (uri == null) {
                "Каталог не вибрано. Натисніть кнопку \"Обрати каталог\"."
            } else {
                countFilesInDocumentTree(context, uri)
            }
        }) {
            Text("Порахувати вибраний каталог")
        }
        Button(onClick = {
            result = createTestFiles(context.filesDir)
        }) {
            Text("Створити тестові файли")
        }
        Button(onClick = {
            result = countFilesInDirectory(context.filesDir.absolutePath)
        }) {
            Text("Порахувати файли у внутрішній папці додатку")
        }
        Button(onClick = {
            result = clearTestFiles(context.filesDir)
        }) {
            Text("Очистити тестові файли")
        }
        Text(
            text = "Вибраний каталог: ${selectedDirectoryUri?.toString() ?: "не вибрано"}"
        )
        ResultText(result)
    }
}

fun countFilesInDocumentTree(context: android.content.Context, uri: Uri?): String {
    if (uri == null) return "Каталог не вибрано."

    val documentTree = DocumentFile.fromTreeUri(context, uri)
        ?: return "Помилка: DocumentFile дорівнює null."

    if (!documentTree.exists() || !documentTree.isDirectory) {
        return "Помилка: немає доступу або вибраний Uri не є каталогом."
    }

    val children = try {
        documentTree.listFiles()
    } catch (error: Exception) {
        return "Помилка: немає доступу до вмісту каталогу."
    }

    if (children.isEmpty()) {
        return "Uri каталогу: $uri\n" +
            "Кількість файлів: 0\n" +
            "Кількість папок: 0\n" +
            "Загальна кількість елементів: 0\n" +
            "Папка порожня."
    }

    val filesCount = children.count { it.isFile }
    val directoriesCount = children.count { it.isDirectory }
    val itemsList = children.joinToString(separator = "\n") { document ->
        val type = if (document.isDirectory) "папка" else "файл"
        "- ${document.name ?: "Без назви"} ($type)"
    }

    return "Uri каталогу: $uri\n" +
        "Кількість файлів: $filesCount\n" +
        "Кількість папок: $directoriesCount\n" +
        "Загальна кількість елементів: ${children.size}\n" +
        "Список елементів:\n$itemsList"
}

fun createTestFiles(directory: File): String {
    return try {
        if (!directory.exists()) directory.mkdirs()
        for (index in 1..3) {
            File(directory, "pract3_test_$index.txt").writeText("Тестовий файл $index")
        }
        "Створено 3 тестові файли.\n\n${countFilesInDirectory(directory.absolutePath)}"
    } catch (error: Exception) {
        "Помилка створення файлів: ${error.message ?: "немає доступу"}."
    }
}

fun clearTestFiles(directory: File): String {
    return try {
        val deletedCount = directory.listFiles()
            ?.filter { it.isFile && it.name.startsWith("pract3_test_") && it.name.endsWith(".txt") }
            ?.count { it.delete() }
            ?: return "Помилка: немає доступу до внутрішньої папки додатку."

        "Видалено тестових файлів: $deletedCount.\n\n${countFilesInDirectory(directory.absolutePath)}"
    } catch (error: Exception) {
        "Помилка очищення файлів: ${error.message ?: "немає доступу"}."
    }
}

fun countFilesInDirectory(path: String): String {
    val clearPath = path.trim()
    if (clearPath.isEmpty()) return "Помилка: введіть шлях до каталогу."

    val directory = File(clearPath)
    if (!directory.exists()) return "Помилка: каталог не існує."
    if (!directory.isDirectory) return "Помилка: вказаний шлях не є каталогом."

    val items = directory.listFiles() ?: return "Помилка: немає доступу до цього каталогу."
    val files = items.filter { it.isFile }
    val directories = items.filter { it.isDirectory }
    val fileList = if (files.isEmpty()) {
        "Файлів немає."
    } else {
        files.joinToString(separator = "\n") { "- ${it.name}" }
    }

    return "Каталог: ${directory.absolutePath}\n" +
        "Кількість файлів: ${files.size}\n" +
        "Кількість підкаталогів: ${directories.size}\n" +
        "Список файлів:\n$fileList"
}

@Composable
fun LibraryScreen() {
    val books = remember { mutableStateListOf<Book>() }
    val readers = remember { mutableStateListOf<Reader>() }
    val history = remember { mutableStateListOf<String>() }

    var bookTitle by remember { mutableStateOf("") }
    var bookAuthor by remember { mutableStateOf("") }
    var bookCountText by remember { mutableStateOf("") }
    var readerName by remember { mutableStateOf("") }
    var selectedReaderName by remember { mutableStateOf("") }
    var selectedBookTitle by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    SectionCard(title = "Рівень 4. Бібліотека") {
        Text("Додати книжку", fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = bookTitle,
            onValueChange = { bookTitle = it },
            label = { Text("Назва") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = bookAuthor,
            onValueChange = { bookAuthor = it },
            label = { Text("Автор") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = bookCountText,
            onValueChange = { bookCountText = it },
            label = { Text("Кількість примірників") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            message = addBook(books, bookTitle, bookAuthor, bookCountText)
            if (!message.startsWith("Помилка")) {
                bookTitle = ""
                bookAuthor = ""
                bookCountText = ""
            }
        }) {
            Text("Додати книжку")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Додати читача", fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = readerName,
            onValueChange = { readerName = it },
            label = { Text("Ім'я читача") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            message = addReader(readers, readerName)
            if (!message.startsWith("Помилка")) readerName = ""
        }) {
            Text("Додати читача")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Видача та повернення", fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = selectedReaderName,
            onValueChange = { selectedReaderName = it },
            label = { Text("Ім'я читача") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = selectedBookTitle,
            onValueChange = { selectedBookTitle = it },
            label = { Text("Назва книжки") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                onClick = {
                    val operation = giveBookToReader(books, readers, selectedReaderName, selectedBookTitle)
                    message = operation.message
                    if (operation.success) history.add(operation.historyText)
                }
            ) {
                Text("Видати")
            }
            Button(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                onClick = {
                    val operation = returnBookToLibrary(books, readers, selectedReaderName, selectedBookTitle)
                    message = operation.message
                    if (operation.success) history.add(operation.historyText)
                }
            ) {
                Text("Повернути")
            }
        }
        ResultText(message)

        Text("Книги", fontWeight = FontWeight.Bold)
        if (books.isEmpty()) {
            Text("Книжок поки немає.")
        } else {
            books.forEach { book ->
                Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp) {
                    Text(
                        text = "${book.title} - ${book.author}. Доступно: ${book.availableCount} з ${book.totalCount}.",
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        Text("Читачі", fontWeight = FontWeight.Bold)
        if (readers.isEmpty()) {
            Text("Читачів поки немає.")
        } else {
            readers.forEach { reader ->
                val booksText = if (reader.borrowedBooks.isEmpty()) {
                    "книжок не взято"
                } else {
                    reader.borrowedBooks.joinToString()
                }
                Text("${reader.name}: $booksText")
            }
        }

        Text("Історія операцій", fontWeight = FontWeight.Bold)
        if (history.isEmpty()) {
            Text("Операцій поки немає.")
        } else {
            history.asReversed().forEach { operation ->
                Text(operation)
            }
        }
    }
}

fun addBook(books: MutableList<Book>, titleText: String, authorText: String, countText: String): String {
    val title = titleText.trim()
    val author = authorText.trim()
    val count = countText.trim().toIntOrNull()

    if (title.isEmpty()) return "Помилка: назва книжки не може бути порожньою."
    if (author.isEmpty()) return "Помилка: автор не може бути порожнім."
    if (count == null || count < 1) return "Помилка: кількість книжок має бути не менше 1."
    if (books.any { it.title.equals(title, ignoreCase = true) }) {
        return "Помилка: така книжка вже є в бібліотеці."
    }

    books.add(Book(title = title, author = author, totalCount = count, availableCount = count))
    return "Книжку додано."
}

fun addReader(readers: MutableList<Reader>, nameText: String): String {
    val name = nameText.trim()

    if (name.isEmpty()) return "Помилка: ім'я читача не може бути порожнім."
    if (readers.any { it.name.equals(name, ignoreCase = true) }) {
        return "Помилка: такий читач уже існує."
    }

    readers.add(Reader(name = name))
    return "Читача додано."
}

data class LibraryOperation(
    val success: Boolean,
    val message: String,
    val historyText: String = ""
)

fun giveBookToReader(
    books: MutableList<Book>,
    readers: MutableList<Reader>,
    readerName: String,
    bookTitle: String
): LibraryOperation {
    val bookIndex = books.indexOfFirst { it.title.equals(bookTitle.trim(), ignoreCase = true) }
    val readerIndex = readers.indexOfFirst { it.name.equals(readerName.trim(), ignoreCase = true) }

    if (bookTitle.trim().isEmpty()) return LibraryOperation(false, "Помилка: введіть назву книжки.")
    if (readerName.trim().isEmpty()) return LibraryOperation(false, "Помилка: введіть ім'я читача.")
    if (bookIndex == -1) return LibraryOperation(false, "Помилка: такої книжки немає.")
    if (readerIndex == -1) return LibraryOperation(false, "Помилка: такого читача немає.")

    val book = books[bookIndex]
    val reader = readers[readerIndex]

    if (book.availableCount <= 0) {
        return LibraryOperation(false, "Помилка: доступних примірників цієї книжки немає.")
    }
    if (reader.borrowedBooks.any { it.equals(book.title, ignoreCase = true) }) {
        return LibraryOperation(false, "Помилка: цей читач уже взяв таку книжку.")
    }

    books[bookIndex] = book.copy(availableCount = book.availableCount - 1)
    val updatedBorrowedBooks = reader.borrowedBooks.toMutableList()
    updatedBorrowedBooks.add(book.title)
    readers[readerIndex] = reader.copy(borrowedBooks = updatedBorrowedBooks)

    return LibraryOperation(
        success = true,
        message = "Книжку видано.",
        historyText = "${reader.name} взяла книгу ${book.title}"
    )
}

fun returnBookToLibrary(
    books: MutableList<Book>,
    readers: MutableList<Reader>,
    readerName: String,
    bookTitle: String
): LibraryOperation {
    val bookIndex = books.indexOfFirst { it.title.equals(bookTitle.trim(), ignoreCase = true) }
    val readerIndex = readers.indexOfFirst { it.name.equals(readerName.trim(), ignoreCase = true) }

    if (bookTitle.trim().isEmpty()) return LibraryOperation(false, "Помилка: введіть назву книжки.")
    if (readerName.trim().isEmpty()) return LibraryOperation(false, "Помилка: введіть ім'я читача.")
    if (bookIndex == -1) return LibraryOperation(false, "Помилка: такої книжки немає.")
    if (readerIndex == -1) return LibraryOperation(false, "Помилка: такого читача немає.")

    val book = books[bookIndex]
    val reader = readers[readerIndex]
    val borrowedBook = reader.borrowedBooks.firstOrNull { it.equals(book.title, ignoreCase = true) }
        ?: return LibraryOperation(false, "Помилка: читач не брав цю книжку.")

    books[bookIndex] = book.copy(availableCount = book.availableCount + 1)
    val updatedBorrowedBooks = reader.borrowedBooks.toMutableList()
    updatedBorrowedBooks.remove(borrowedBook)
    readers[readerIndex] = reader.copy(borrowedBooks = updatedBorrowedBooks)

    return LibraryOperation(
        success = true,
        message = "Книжку повернуто.",
        historyText = "${reader.name} повернула книгу ${book.title}"
    )
}

@Composable
fun ResultText(text: String) {
    if (text.isNotBlank()) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )
    }
}
