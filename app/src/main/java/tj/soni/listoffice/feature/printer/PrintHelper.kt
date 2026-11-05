package tj.soni.listoffice.feature.printer

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.graphics.pdf.PdfRenderer
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/** Чопи PDF тавассути Android Print Framework (интихоби принтер, нусха, ранг, ориентатсия — дар диалоги системавӣ). */
object PrintHelper {

    fun printPdf(context: Context, file: File, jobName: String = file.nameWithoutExtension): Boolean {
        val pm = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return false
        if (!file.exists()) return false
        val pages = pageCount(file)

        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(file.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(if (pages > 0) pages else PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pageRanges: Array<out PageRange>,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                try {
                    FileInputStream(file).use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }
        val attrs = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .build()
        pm.print(jobName, adapter, attrs)
        return true
    }

    private fun pageCount(file: File): Int = try {
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val r = PdfRenderer(pfd)
        try { r.pageCount } finally { r.close(); pfd.close() }
    } catch (e: Exception) {
        -1
    }
}
