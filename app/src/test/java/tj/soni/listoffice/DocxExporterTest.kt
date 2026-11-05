package tj.soni.listoffice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tj.soni.listoffice.core.export.DocxExporter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class DocxExporterTest {
    private fun read(bytes: ByteArray): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { z ->
            var e = z.nextEntry
            while (e != null) {
                map[e.name] = z.readBytes().toString(Charsets.UTF_8)
                e = z.nextEntry
            }
        }
        return map
    }

    @Test fun docx_hasRequiredParts_andEscapes() {
        val bos = ByteArrayOutputStream()
        DocxExporter.write(bos, "Салом & <тест>\u000CСаҳифаи 2", 14)
        val parts = read(bos.toByteArray())
        assertTrue(parts.containsKey("[Content_Types].xml"))
        assertTrue(parts.containsKey("_rels/.rels"))
        val doc = parts["word/document.xml"] ?: ""
        assertTrue(doc.contains("Салом &amp; &lt;тест&gt;"))
        assertTrue(doc.contains("w:br w:type=\"page\""))
        assertTrue(doc.contains("Саҳифаи 2"))
    }

    @Test fun escape_stripsControlChars() {
        assertEquals("ab", DocxExporter.escape("a\u0001b"))
    }
}
