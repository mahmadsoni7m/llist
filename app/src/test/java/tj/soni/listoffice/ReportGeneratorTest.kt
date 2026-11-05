package tj.soni.listoffice

import org.junit.Assert.assertTrue
import org.junit.Test
import tj.soni.listoffice.domain.Lang
import tj.soni.listoffice.domain.ReportGenerator
import tj.soni.listoffice.domain.ReportType

class ReportGeneratorTest {
    @Test fun tajik_hasAllSections() {
        val t = ReportGenerator.generate(ReportType.REFERAT, Lang.TG, "Экология", "Ном", "Донишгоҳ")
        listOf("реферат", "нақша", "муқаддима", "қисми асосӣ", "хулоса", "рӯйхати адабиёт", "экология").forEach {
            assertTrue("missing $it", t.lowercase().contains(it))
        }
        assertTrue(t.contains(ReportGenerator.PAGE_BREAK))
    }

    @Test fun russianAndEnglish_hasSections() {
        val ru = ReportGenerator.generate(ReportType.KURSI, Lang.RU, "Тема", "", "")
        val en = ReportGenerator.generate(ReportType.INSHO, Lang.EN, "Topic", "", "")
        assertTrue(ru.lowercase().contains("заключение") && ru.lowercase().contains("введение"))
        assertTrue(en.lowercase().contains("conclusion") && en.lowercase().contains("references"))
    }
}
