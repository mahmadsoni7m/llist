package tj.soni.listoffice.feature.filemanager

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tj.soni.listoffice.core.ui.shareFile
import tj.soni.listoffice.feature.printer.PrintHelper
import java.io.File

private const val MAX_VIEW_PAGES = 40

@Composable
fun PdfViewerScreen(file: File, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var pages by remember(file) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var error by remember(file) { mutableStateOf<String?>(null) }
    var loading by remember(file) { mutableStateOf(true) }

    BackHandler(onBack = onClose)
    LaunchedEffect(file) {
        try {
            pages = withContext(Dispatchers.IO) { renderPdf(file) }
        } catch (e: Exception) {
            error = "PDF кушода нашуд: ${e.message ?: e.javaClass.simpleName}"
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, contentDescription = "Бозгашт") }
            Text(file.name, modifier = Modifier.weight(1f), maxLines = 1)
            IconButton(onClick = { PrintHelper.printPdf(ctx, file) }) { Icon(Icons.Default.Print, contentDescription = "Чоп") }
            IconButton(onClick = { shareFile(ctx, file) }) { Icon(Icons.Default.Share, contentDescription = "Мубодила") }
        }
        when {
            error != null -> Text(error ?: "", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
            loading -> Text("Боршавӣ...", modifier = Modifier.padding(16.dp))
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                items(pages) { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

private fun renderPdf(file: File): List<Bitmap> {
    val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    val renderer = PdfRenderer(pfd)
    try {
        val out = ArrayList<Bitmap>()
        val n = minOf(renderer.pageCount, MAX_VIEW_PAGES)
        for (i in 0 until n) {
            val page = renderer.openPage(i)
            try {
                val w = 1000
                val h = (w.toFloat() * page.height / page.width).toInt()
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                out.add(bmp)
            } finally {
                page.close()
            }
        }
        return out
    } finally {
        renderer.close()
        pfd.close()
    }
}
