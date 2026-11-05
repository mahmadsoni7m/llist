package tj.soni.listoffice

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tj.soni.listoffice.core.export.DocxExporter
import tj.soni.listoffice.core.export.PdfExporter
import tj.soni.listoffice.core.ui.openExternal
import tj.soni.listoffice.data.FileRepository
import tj.soni.listoffice.domain.Lang
import tj.soni.listoffice.domain.ReportGenerator
import tj.soni.listoffice.domain.ReportType
import tj.soni.listoffice.domain.TextTools
import tj.soni.listoffice.feature.handwriting.HandwritingRenderer
import tj.soni.listoffice.feature.handwriting.HwParams
import tj.soni.listoffice.feature.handwriting.Paper
import tj.soni.listoffice.feature.printer.PrintHelper
import java.io.File

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = FileRepository(app)
    val renderer = HandwritingRenderer(app)

    private val _files = MutableStateFlow<List<File>>(emptyList())
    val files: StateFlow<List<File>> = _files.asStateFlow()

    var tab by mutableIntStateOf(0)
    var viewing by mutableStateOf<File?>(null)
    var message by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)

    // Document Studio
    var docName by mutableStateOf("Ҳуҷҷати нав")
    var docText by mutableStateOf("")
    var docFontSize by mutableIntStateOf(1) // индекс дар docFontSizes
    val docFontSizes = listOf(12, 14, 16)
    private var saveJob: Job? = null

    // Handwriting Studio
    var hwText by mutableStateOf("")
    var hwStyle by mutableIntStateOf(0)
    var hwPaper by mutableIntStateOf(1)
    var hwMargin by mutableStateOf(true)
    var hwCustomSize by mutableFloatStateOf(46f)
    var hwCustomRot by mutableFloatStateOf(2f)
    var hwCustomJitter by mutableFloatStateOf(2f)
    var hwPreview by mutableStateOf<List<Bitmap>>(emptyList())
    var fontWarning by mutableStateOf<String?>(null)

    init {
        refresh()
        viewModelScope.launch(Dispatchers.Default) {
            val bad = listOf("natural.ttf", "beautiful.ttf", "clean.ttf").filter { !renderer.coversTajik(it) }
            withContext(Dispatchers.Main) {
                fontWarning = if (bad.isEmpty()) null else
                    "Шрифтҳои дастнавис (${bad.joinToString()}) дар APK нестанд ё кириллиро надоранд, барои ҳамин хати оддии системавӣ истифода мешавад. " +
                        "Дар GitHub Actions шрифтҳо худкор зеркашӣ мешаванд; build-ро аз нав иҷро кунед ва APK-и навро насб намоед."
            }
        }
    }

    fun refresh() {
        _files.value = repo.list()
    }

    // ---------- Document Studio ----------

    fun onDocTextChange(t: String) {
        docText = t
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(1500)
            withContext(Dispatchers.IO) { repo.saveText(docName, docText) }
            refresh()
        }
    }

    fun saveDoc() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.saveText(docName, docText) }
            refresh()
            message = "Нигоҳ дошта шуд"
        }
    }

    fun newDoc() {
        docName = "Ҳуҷҷати нав"
        docText = ""
    }

    fun applyTemplate(type: ReportType, lang: Lang, topic: String, author: String, org: String) {
        docName = "${type.title(lang)} - ${topic.trim().take(40)}".trim()
        docText = ReportGenerator.generate(type, lang, topic, author, org)
        hwText = docText
        saveDoc()
    }

    fun openFile(context: Context, f: File) {
        when (f.extension.lowercase()) {
            "pdf" -> viewing = f
            "txt" -> {
                viewModelScope.launch {
                    val t = withContext(Dispatchers.IO) { repo.read(f) }
                    docName = f.nameWithoutExtension
                    docText = t
                    tab = 1
                }
            }
            else -> if (!openExternal(context, f)) {
                message = "Барномае барои кушодани .${f.extension} ёфт нашуд"
            }
        }
    }

    fun exportPdf(onDone: (File) -> Unit) {
        runIo("PDF сохта нашуд", {
            val f = repo.fileFor(docName, "pdf")
            PdfExporter.textToPdf(docText, f, docFontSizes[docFontSize].toFloat())
            f
        }) { f ->
            message = "PDF сохта шуд: ${f.name}"
            onDone(f)
        }
    }

    fun exportDocx() {
        runIo("DOCX сохта нашуд", {
            val f = repo.fileFor(docName, "docx")
            f.outputStream().use { DocxExporter.write(it, docText, docFontSizes[docFontSize]) }
            f
        }) { f -> message = "DOCX сохта шуд: ${f.name}" }
    }

    fun printDocument(context: Context) {
        if (docText.isBlank()) {
            message = "Ҳуҷҷат холӣ аст"
            return
        }
        runIo("Чоп омода нашуд", {
            val f = File(repo.cacheDir, "print.pdf")
            PdfExporter.textToPdf(docText, f, docFontSizes[docFontSize].toFloat())
            f
        }) { f -> printPdfFile(context, f, docName) }
    }

    fun summarize(): String = TextTools.summarize(docText, 3)

    fun cleanText() {
        docText = TextTools.cleanSpaces(docText)
    }

    fun toHandwriting() {
        hwText = docText
        tab = 2
    }

    // ---------- Handwriting Studio ----------

    fun hwParams(): HwParams = when (hwStyle) {
        0 -> HwParams("natural.ttf", 58f, 2.0f, 2.5f, 0.6f)
        1 -> HwParams("beautiful.ttf", 50f, 1.0f, 1.0f, 1.2f)
        2 -> HwParams("clean.ttf", 46f, 0.4f, 0.3f, 0.6f)
        else -> HwParams("natural.ttf", hwCustomSize, hwCustomRot, hwCustomJitter, 0.8f)
    }

    fun previewHandwriting() {
        if (hwText.isBlank()) {
            message = "Аввал матн ворид кунед"
            return
        }
        runIo("Пешнамоиш сохта нашуд", {
            val list = ArrayList<Bitmap>()
            renderer.render(hwText, hwParams(), Paper.values()[hwPaper], hwMargin, 0.5f, 3) { list.add(it) }
            list as List<Bitmap>
        }) { list -> hwPreview = list }
    }

    private fun buildHandwritingPdf(): File {
        val f = repo.fileFor("Дастнавис-${System.currentTimeMillis() / 1000}", "pdf")
        renderer.exportPdf(hwText, hwParams(), Paper.values()[hwPaper], hwMargin, f)
        return f
    }

    fun exportHandwritingPdf() {
        if (hwText.isBlank()) {
            message = "Аввал матн ворид кунед"
            return
        }
        runIo("PDF-и дастнавис сохта нашуд", { buildHandwritingPdf() }) { f ->
            message = "PDF сохта шуд: ${f.name}"
        }
    }

    fun printHandwriting(context: Context) {
        if (hwText.isBlank()) {
            message = "Аввал матн ворид кунед"
            return
        }
        runIo("Чоп омода нашуд", {
            val f = File(repo.cacheDir, "handwriting.pdf")
            renderer.exportPdf(hwText, hwParams(), Paper.values()[hwPaper], hwMargin, f)
            f
        }) { f -> printPdfFile(context, f, "Дастнавис") }
    }

    // ---------- File manager ----------

    fun rename(f: File, newName: String) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { repo.rename(f, newName) }
            message = if (ok) "Ном тағйир ёфт" else "Номи нав аллакай мавҷуд аст ё нодуруст аст"
            refresh()
        }
    }

    fun delete(f: File) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { repo.delete(f) }
            message = if (ok) "Нест карда шуд" else "Нест карда нашуд"
            refresh()
        }
    }

    fun importFrom(uri: Uri) {
        runIo("Ворид кардан муяссар нашуд", { repo.importFrom(uri) }) { f ->
            message = if (f != null) "Ворид шуд: ${f.name}" else "Файл хонда нашуд"
            refresh()
        }
    }

    fun exportTo(f: File, uri: Uri) {
        runIo("Содирот муяссар нашуд", { repo.exportTo(f, uri) }) { ok ->
            message = if (ok) "Содирот анҷом ёфт" else "Содирот муяссар нашуд"
        }
    }

    fun printFile(context: Context, f: File) {
        when (f.extension.lowercase()) {
            "pdf" -> printPdfFile(context, f, f.nameWithoutExtension)
            "txt" -> runIo("Чоп омода нашуд", {
                val out = File(repo.cacheDir, "file-print.pdf")
                PdfExporter.textToPdf(repo.read(f), out, docFontSizes[docFontSize].toFloat())
                out
            }) { out -> printPdfFile(context, out, f.nameWithoutExtension) }
            else -> message = "Ин формат мустақиман чоп намешавад. Ҳуҷҷатро ба PDF содир кунед."
        }
    }

    // ---------- helpers ----------

    private fun printPdfFile(context: Context, f: File, name: String) {
        val ok = PrintHelper.printPdf(context, f, name)
        if (!ok) message = "Хидмати чоп дар ин дастгоҳ дастрас нест. Принтер ё плагини чопро дар танзимоти Android фаъол кунед."
    }

    private fun <T> runIo(errorText: String, work: () -> T, done: (T) -> Unit) {
        viewModelScope.launch {
            busy = true
            try {
                val r = withContext(Dispatchers.IO) { work() }
                refresh()
                done(r)
            } catch (e: Exception) {
                message = "$errorText: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                busy = false
            }
        }
    }
}
