package tj.soni.listoffice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import tj.soni.listoffice.domain.TextTools
import org.junit.Test

class TextToolsTest {
    @Test fun countMatches_ignoresCase() {
        assertEquals(2, TextTools.countMatches("Салом салом ҷаҳон", "салом"))
        assertEquals(0, TextTools.countMatches("abc", ""))
    }

    @Test fun wordCount_works() {
        assertEquals(3, TextTools.wordCount("  як  ду\nсе "))
    }

    @Test fun cleanSpaces_collapses() {
        assertEquals("а б\n\nв", TextTools.cleanSpaces("а   б  \n\n\n\nв"))
    }

    @Test fun summarize_limitsSentences() {
        val text = "Барномасозӣ ин санъати навиштани барномаҳо мебошад. " +
            "Барномасозӣ ба донишҷӯён кӯмак мекунад. " +
            "Ҳаво имрӯз хеле хуб ва офтобӣ мебошад. " +
            "Барномасозӣ дар ҳаёти имрӯза хеле муҳим аст."
        val s = TextTools.summarize(text, 2)
        assertTrue(s.isNotBlank())
        assertTrue(s.split(Regex("(?<=[.!?])\\s+")).size <= 2)
    }
}
