package tj.soni.listoffice.feature.handwriting

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.util.Random

enum class Paper(val label: String) {
    WHITE("Қоғази сафед A4"),
    RULED("Дафтар (хатдор)"),
    GRID("Дафтар (katak)")
}

data class HwParams(
    val fontAsset: String,
    val sizePx: Float,
    val rotationDeg: Float,
    val jitterPx: Float,
    val spacingPx: Float
)

/** Матнро ба саҳифаҳои A4 бо дастнависи кабуд (ручка) табдил медиҳад. */
class HandwritingRenderer(private val context: Context) {

    companion object {
        const val W = 1240
        const val H = 1754
        const val TAJIK_LETTERS = "ҒғҚқҲҳҶҷӮӯӢӣ"
        private const val INK = 0xFF1A3FB5L
    }

    fun typeface(asset: String): Typeface? = try {
        Typeface.createFromAsset(context.assets, "fonts/$asset")
    } catch (e: Exception) {
        null
    }

    /** true, агар шрифт ҳамаи ҳарфҳои тоҷикиро дошта бошад. */
    fun coversTajik(asset: String): Boolean {
        val tf = typeface(asset) ?: return false
        val p = Paint().apply { typeface = tf }
        return TAJIK_LETTERS.all { p.hasGlyph(it.toString()) }
    }

    /**
     * Саҳифаҳоро яке-яке тавлид мекунад ва ба [onPage] медиҳад (bitmap-ро қабулкунанда recycle мекунад).
     * [scale] — андозаи bitmap нисбат ба 1240x1754. [maxPages] — ҳадди саҳифаҳо. Шумораи саҳифаҳоро бармегардонад.
     */
    fun render(
        text: String,
        params: HwParams,
        paper: Paper,
        marginLine: Boolean,
        scale: Float,
        maxPages: Int,
        onPage: (Bitmap) -> Unit
    ): Int {
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK.toInt()
            textSize = params.sizePx
            typeface = typeface(params.fontAsset) ?: Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
        }
        val fallback = Paint(ink).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
        }
        val left = if (marginLine) 190f else 120f
        val right = W - 110f
        val top = 170f
        val bottom = H - 140f
        val lineH = if (paper == Paper.GRID) 64f else maxOf(60f, params.sizePx * 1.35f)
        val spaceW = ink.measureText(" ") + params.spacingPx * 2
        val rnd = Random(text.hashCode().toLong())

        var count = 0
        var bmp = Bitmap.createBitmap((W * scale).toInt(), (H * scale).toInt(), Bitmap.Config.ARGB_8888)
        var cv = Canvas(bmp)
        cv.scale(scale, scale)
        drawBackground(cv, paper, marginLine, left, top, bottom, lineH)
        var y = top + lineH - 8f

        fun nextPage(): Boolean {
            onPage(bmp)
            count++
            if (count >= maxPages) return false
            bmp = Bitmap.createBitmap((W * scale).toInt(), (H * scale).toInt(), Bitmap.Config.ARGB_8888)
            cv = Canvas(bmp)
            cv.scale(scale, scale)
            drawBackground(cv, paper, marginLine, left, top, bottom, lineH)
            y = top + lineH - 8f
            return true
        }

        fun ensure(): Boolean = if (y > bottom) nextPage() else true

        var stopped = false
        outer@ for (para in text.replace("\r", "").replace('\u000C', '\n').split("\n")) {
            val words = para.split(" ").filter { it.isNotEmpty() }
            if (words.isEmpty()) {
                y += lineH
                continue
            }
            var x = left
            for (word in words) {
                val ww = ink.measureText(word) + word.length * params.spacingPx
                if (x > left && x + ww > right) {
                    x = left
                    y += lineH
                }
                if (!ensure()) { stopped = true; break@outer }
                for (ch in word) {
                    val s = ch.toString()
                    val paint = if (ink.hasGlyph(s)) ink else fallback
                    val cw = paint.measureText(s)
                    if (x + cw > right) {
                        x = left
                        y += lineH
                        if (!ensure()) { stopped = true; break@outer }
                    }
                    val drift = (Math.sin(x / 90.0 + y / 50.0) * params.jitterPx * 0.6).toFloat()
                    val dy = (rnd.nextFloat() * 2f - 1f) * params.jitterPx * 0.5f + drift
                    val rot = (rnd.nextFloat() * 2f - 1f) * params.rotationDeg
                    paint.alpha = 215 + rnd.nextInt(40)
                    cv.save()
                    cv.translate(x, y + dy)
                    cv.rotate(rot)
                    cv.drawText(s, 0f, 0f, paint)
                    cv.restore()
                    x += cw + params.spacingPx * (0.6f + rnd.nextFloat() * 0.8f)
                }
                x += spaceW
            }
            y += lineH
        }
        if (!stopped) {
            onPage(bmp)
            count++
        }
        return count
    }

    private fun drawBackground(
        c: Canvas, paper: Paper, marginLine: Boolean,
        left: Float, top: Float, bottom: Float, lineH: Float
    ) {
        c.drawColor(Color.WHITE)
        val line = Paint().apply {
            color = 0xFFB7D0E8.toInt()
            strokeWidth = 2f
        }
        when (paper) {
            Paper.WHITE -> Unit
            Paper.RULED -> {
                var yy = top + lineH
                while (yy < H - 60f) {
                    c.drawLine(0f, yy, W.toFloat(), yy, line)
                    yy += lineH
                }
            }
            Paper.GRID -> {
                var yy = top
                while (yy < H - 60f) {
                    c.drawLine(0f, yy, W.toFloat(), yy, line)
                    yy += 32f
                }
                var xx = 0f
                while (xx < W) {
                    c.drawLine(xx, 0f, xx, H.toFloat(), line)
                    xx += 32f
                }
            }
        }
        if (marginLine) {
            val m = Paint().apply {
                color = 0xFFE57373.toInt()
                strokeWidth = 3f
            }
            c.drawLine(left - 30f, 0f, left - 30f, H.toFloat(), m)
        }
    }

    /** PDF-и чопшавандаи A4 месозад. Шумораи саҳифаҳоро бармегардонад. */
    fun exportPdf(
        text: String, params: HwParams, paper: Paper, marginLine: Boolean, out: File, maxPages: Int = 50
    ): Int {
        val doc = PdfDocument()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        var n = 0
        render(text, params, paper, marginLine, 1f, maxPages) { bmp ->
            n++
            val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, n).create())
            page.canvas.drawBitmap(bmp, null, RectF(0f, 0f, 595f, 842f), paint)
            doc.finishPage(page)
            bmp.recycle()
        }
        FileOutputStream(out).use { doc.writeTo(it) }
        doc.close()
        return n
    }
}
