package tj.soni.listoffice.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

fun mimeOf(file: File): String = when (file.extension.lowercase()) {
    "pdf" -> "application/pdf"
    "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    "txt" -> "text/plain"
    else -> "*/*"
}

private fun uriOf(context: Context, file: File) =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

fun shareFile(context: Context, file: File): Boolean = try {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mimeOf(file)
        putExtra(Intent.EXTRA_STREAM, uriOf(context, file))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, file.name))
    true
} catch (e: Exception) {
    false
}

fun openExternal(context: Context, file: File): Boolean = try {
    val view = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uriOf(context, file), mimeOf(file))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(view)
    true
} catch (e: ActivityNotFoundException) {
    false
} catch (e: Exception) {
    false
}
