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

/** Тарҳбандии саҳифа: ҳошияҳо бо миллиметр (чап, рост, боло, поён) ва имконоти иловагӣ. */
data class HwLayout(
    val leftMm: Float = 20f,
    val rightMm: Float = 10f,
    val topMm: Float = 15f,
    val bottomMm: Float = 15f,
    val marginLine: Boolean = true,
    val indent: Boolean = true,
    val centerTitles: Boolean = true,
    val pageNumbers: Boolean = true
)

/** Матнро ба саҳифаҳои A4 бо дастнависи кабуд (ручка) табдил медиҳад. */
class HandwritingRenderer(private val context: Context) {

    companion object {
        const val W = 1240
        const val H = 1754
        const val PX_PER_MM = 1240f / 210f
        const val CYRILLIC_BASE = "абвгдеёжзийклмнопрстуфхцчшщъыьэюяАБВГДЕЖЗИКЛМНОПРСТУФХЦЧШЩЭЮЯ"
        private const val INK = 0xFF1A3FB5L
        private const val INDENT_PX = 90f
    }

    fun typeface(asset: String): Typeface? = try {
        Typeface.createFromAsset(context.assets, "fonts/$asset")
    } catch (e: Exception) {
        null
    }

    /**
     * true, агар шрифт мавҷуд бошад ва кириллии асосиро дошта бошад.
     * Ҳарфҳои тоҷикӣ (Ғ Қ Ҳ Ҷ Ӣ Ӯ) аз ҳарфи асосӣ + нишона худи барнома сохта мешаванд.
     */
    fun coversTajik(asset: String): Boolean {
        val tf = typeface(asset) ?: return false
        val p = Paint().apply { typeface = tf }
        return CYRILLIC_BASE.all { p.hasGlyph(it.toString()) }
    }

    private fun composeBase(ch: Char): Pair<Char, String>? = when (ch) {
        'Ғ' -> Pair('Г', "bar")
        'ғ' -> Pair('г', "bar")
        'Қ' -> Pair('К', "tail")
        'қ' -> Pair('к', "tail")
        'Ҳ' -> Pair('Х', "tail")
        'ҳ' -> Pair('х', "tail")
        'Ҷ' -> Pair('Ч', "tail")
        'ҷ' -> Pair('ч', "tail")
        'Ӣ' -> Pair('И', "macron")
        'ӣ' -> Pair('и', "macron")
        'Ӯ' -> Pair('У', "macron")
        'ӯ' -> Pair('у', "macron")
        else -> null
    }

    private fun drawMark(c: Canvas, mark: String, upper: Boolean, cw: Float, p: Paint) {
        val sz = p.textSize
        val m = Paint(p).apply {
            style = Paint.Style.STROKE
            strokeWidth = maxOf(2.5f, sz * 0.055f)
            strokeCap = Paint.Cap.ROUND
        }
        when (mark) {
            "macron" -> {
                val y = if (upper) -sz * 0.95f else -sz * 0.68f
                c.drawLine(cw * 0.2f, y, cw * 0.85f, y, m)
            }
            "bar" -> {
                val y = if (upper) -sz * 0.38f else -sz * 0.25f
                c.drawLine(-cw * 0.02f, y, cw * 0.6f, y, m)
            }
            else -> {
                c.drawLine(cw * 0.8f, 0f, cw * 0.9f, sz * 0.26f, m)
            }
        }
    }

    private fun isTitle(para: String): Boolean {
        val t = para.trim()
        return t.length in 1..60 && t.any { it.isLetter() } && t.none { it.isLowerCase() }
    }

    /**
     * Саҳифаҳоро яке-яке тавлид мекунад ва ба [onPage] медиҳад (қабулкунанда bitmap-ро recycle мекунад).
     * [scale] — андозаи bitmap нисбат ба 1240x1754. [maxPages] — ҳадди саҳифаҳо. Шумораи саҳифаҳоро бармегардонад.
     */
    fun render(
        text: String,
        params: HwParams,
        paper: Paper,
        layout: HwLayout,
        scale: Float,
        maxPages: Int,
        onPage: (Bitmap) -> Unit
    ): Int {
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK.toInt()
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = params.sizePx * 0.012f
            textSize = params.sizePx
            typeface = typeface(params.fontAsset) ?: Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
        }
        val fallback = Paint(ink).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
        }
        val left = (layout.leftMm * PX_PER_MM).coerceIn(0f, W * 0.5f)
        val right = maxOf(W - layout.rightMm * PX_PER_MM, left + 300f)
        val top = (layout.topMm * PX_PER_MM).coerceIn(0f, H * 0.4f)
        val bottom = maxOf(H - layout.bottomMm * PX_PER_MM, top + 300f)
        val lineH = if (paper == Paper.GRID) 64f else maxOf(60f, params.sizePx * 1.35f)
        val spaceW = ink.measureText(" ") + params.spacingPx * 2
        val rnd = Random(text.hashCode().toLong())

        var count = 0
        var bmp = Bitmap.createBitmap((W * scale).toInt(), (H * scale).toInt(), Bitmap.Config.ARGB_8888)
        var cv = Canvas(bmp)
        cv.scale(scale, scale)
        drawBackground(cv, paper, layout.marginLine, left, top, lineH)
        var y = top + lineH - 8f

        fun finishPage() {
            if (layout.pageNumbers) {
                val num = (count + 1).toString()
                val numPaint = Paint(ink).apply { textSize = params.sizePx * 0.8f; alpha = 230 }
                val ny = H - maxOf(30f, layout.bottomMm * PX_PER_MM * 0.45f)
                cv.drawText(num, (W - numPaint.measureText(num)) / 2f, ny, numPaint)
            }
            onPage(bmp)
            count++
        }

        fun nextPage(): Boolean {
            finishPage()
            if (count >= maxPages) return false
            bmp = Bitmap.createBitmap((W * scale).toInt(), (H * scale).toInt(), Bitmap.Config.ARGB_8888)
            cv = Canvas(bmp)
            cv.scale(scale, scale)
            drawBackground(cv, paper, layout.marginLine, left, top, lineH)
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
            val centered = layout.centerTitles && isTitle(para)
            var x = left
            if (centered) {
                val tw = ink.measureText(words.joinToString(" ")) + para.length * params.spacingPx
                x = if (tw < right - left) left + (right - left - tw) / 2f else left
            } else if (layout.indent) {
                x = left + INDENT_PX
            }
            for (word in words) {
                val ww = ink.measureText(word) + word.length * params.spacingPx
                if (x > left && x + ww > right && !centered) {
                    x = left
                    y += lineH
                }
                if (!ensure()) { stopped = true; break@outer }
                val wordDy = (rnd.nextFloat() * 2f - 1f) * params.jitterPx * 0.8f
                for (ch in word) {
                    val s = ch.toString()
                    val comp = if (!ink.hasGlyph(s)) composeBase(ch) else null
                    val drawS = comp?.first?.toString() ?: s
                    val paint = if (ink.hasGlyph(drawS)) ink else fallback
                    val cw = paint.measureText(drawS)
                    if (x + cw > right) {
                        x = left
                        y += lineH
                        if (!ensure()) { stopped = true; break@outer }
                    }
                    val drift = (Math.sin(x / 90.0 + y / 50.0) * params.jitterPx * 0.6).toFloat()
                    val dy = (rnd.nextFloat() * 2f - 1f) * params.jitterPx * 0.5f + drift + wordDy
                    val rot = (rnd.nextFloat() * 2f - 1f) * params.rotationDeg
                    paint.alpha = 215 + rnd.nextInt(40)
                    cv.save()
                    cv.translate(x, y + dy)
                    cv.rotate(rot)
                    cv.skew((rnd.nextFloat() * 2f - 1f) * 0.06f, 0f)
                    cv.drawText(drawS, 0f, 0f, paint)
                    if (comp != null && paint === ink) drawMark(cv, comp.second, ch.isUpperCase(), cw, paint)
                    cv.restore()
                    x += cw + params.spacingPx * (0.6f + rnd.nextFloat() * 0.8f)
                }
                x += spaceW
            }
            y += lineH
        }
        if (!stopped) finishPage()
        return count
    }

    private fun drawBackground(
        c: Canvas, paper: Paper, marginLine: Boolean,
        left: Float, top: Float, lineH: Float
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
            val x = maxOf(left - 14f, 6f)
            c.drawLine(x, 0f, x, H.toFloat(), m)
        }
    }

    /** PDF-и чопшавандаи A4 месозад. Шумораи саҳифаҳоро бармегардонад. */
    fun exportPdf(
        text: String, params: HwParams, paper: Paper, layout: HwLayout, out: File, maxPages: Int = 50
    ): Int {
        val doc = PdfDocument()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        var n = 0
        render(text, params, paper, layout, 1f, maxPages) { bmp ->
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
