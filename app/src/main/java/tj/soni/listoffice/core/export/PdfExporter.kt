package tj.soni.listoffice.core.export

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    /** Матнро ба PDF-и A4 бо рақамгузории саҳифа табдил медиҳад. Шумораи саҳифаҳоро бармегардонад. */
    fun textToPdf(text: String, out: File, fontSize: Float = 14f): Int {
        val doc = PdfDocument()
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = fontSize
            typeface = Typeface.SERIF
        }
        val numPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }
        val pw = 595
        val ph = 842
        val left = 85
        val top = 56
        val right = 42
        val bottom = 60
        val width = pw - left - right
        val usable = ph - top - bottom
        var pageNo = 0

        for (segment in text.replace("\r", "").split('\u000C')) {
            val layout = StaticLayout.Builder.obtain(segment, 0, segment.length, paint, width)
                .setLineSpacing(0f, 1.25f)
                .setIncludePad(false)
                .build()
            var line = 0
            do {
                val startTop = layout.getLineTop(line)
                var end = line
                while (end < layout.lineCount && layout.getLineBottom(end) - startTop <= usable) end++
                if (end == line) end = line + 1
                pageNo++
                val page = doc.startPage(PdfDocument.PageInfo.Builder(pw, ph, pageNo).create())
                val c = page.canvas
                c.save()
                c.translate(left.toFloat(), top.toFloat() - startTop)
                c.clipRect(0f, startTop.toFloat(), width.toFloat(), layout.getLineBottom(end - 1).toFloat())
                layout.draw(c)
                c.restore()
                c.drawText(pageNo.toString(), pw / 2f, ph - 28f, numPaint)
                doc.finishPage(page)
                line = end
            } while (line < layout.lineCount)
        }
        FileOutputStream(out).use { doc.writeTo(it) }
        doc.close()
        return pageNo
    }
}
