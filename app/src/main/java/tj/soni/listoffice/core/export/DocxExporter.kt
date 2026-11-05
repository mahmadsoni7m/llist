package tj.soni.listoffice.core.export

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** DOCX-и воқеӣ (OOXML) бе китобхонаи берунӣ. \u000C = канори саҳифа. */
object DocxExporter {

    fun escape(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in s) {
            when {
                ch == '&' -> sb.append("&amp;")
                ch == '<' -> sb.append("&lt;")
                ch == '>' -> sb.append("&gt;")
                ch == '"' -> sb.append("&quot;")
                ch.code < 0x20 && ch != '\t' -> Unit
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun documentXml(text: String, fontSizePt: Int): String {
        val half = fontSizePt * 2
        val rpr = "<w:rPr><w:rFonts w:ascii=\"Times New Roman\" w:hAnsi=\"Times New Roman\" w:cs=\"Times New Roman\"/><w:sz w:val=\"$half\"/><w:szCs w:val=\"$half\"/></w:rPr>"
        val body = StringBuilder()
        text.replace("\r", "").split('\u000C').forEachIndexed { i, page ->
            if (i > 0) body.append("<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>")
            for (line in page.split("\n")) {
                body.append("<w:p><w:r>").append(rpr)
                    .append("<w:t xml:space=\"preserve\">").append(escape(line)).append("</w:t></w:r></w:p>")
            }
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>" +
            body +
            "<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/>" +
            "<w:pgMar w:top=\"1134\" w:right=\"850\" w:bottom=\"1134\" w:left=\"1701\" w:header=\"708\" w:footer=\"708\" w:gutter=\"0\"/></w:sectPr>" +
            "</w:body></w:document>"
    }

    fun write(out: OutputStream, text: String, fontSizePt: Int = 14) {
        ZipOutputStream(out).use { z ->
            put(z, "[Content_Types].xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                    "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                    "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                    "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>" +
                    "</Types>")
            put(z, "_rels/.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>" +
                    "</Relationships>")
            put(z, "word/document.xml", documentXml(text, fontSizePt))
        }
    }

    private fun put(z: ZipOutputStream, name: String, content: String) {
        z.putNextEntry(ZipEntry(name))
        z.write(content.toByteArray(Charsets.UTF_8))
        z.closeEntry()
    }
}
