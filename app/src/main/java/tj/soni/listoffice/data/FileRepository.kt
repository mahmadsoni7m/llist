package tj.soni.listoffice.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/** Ҳамаи файлҳои барнома дар filesDir/files нигоҳ дошта мешаванд. */
class FileRepository(private val context: Context) {

    val dir: File
        get() = File(context.filesDir, "files").also { if (!it.exists()) it.mkdirs() }

    val cacheDir: File
        get() = File(context.cacheDir, "print").also { if (!it.exists()) it.mkdirs() }

    fun list(): List<File> =
        dir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun fileFor(name: String, ext: String): File {
        var base = name.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_")
        base = base.replace(Regex("\\.(txt|pdf|docx|pptx)$", RegexOption.IGNORE_CASE), "")
        if (base.isEmpty()) base = "Ҳуҷҷат"
        return File(dir, "$base.$ext")
    }

    fun saveText(name: String, text: String): File {
        val f = fileFor(name, "txt")
        f.writeText(text, Charsets.UTF_8)
        return f
    }

    fun read(file: File): String = file.readText(Charsets.UTF_8)

    fun rename(file: File, newName: String): Boolean {
        val target = fileFor(newName, file.extension)
        if (target.exists()) return false
        return file.renameTo(target)
    }

    fun delete(file: File): Boolean = file.delete()

    fun importFrom(uri: Uri): File? {
        var name = "import_${System.currentTimeMillis()}"
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx)?.let { name = it }
        }
        val clean = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val target = File(dir, clean)
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { out -> input.copyTo(out) }
        } ?: return null
        return target
    }

    fun exportTo(file: File, uri: Uri): Boolean {
        val out = context.contentResolver.openOutputStream(uri) ?: return false
        out.use { o -> file.inputStream().use { it.copyTo(o) } }
        return true
    }
}
