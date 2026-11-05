package tj.soni.listoffice.domain

import java.util.Calendar

enum class Lang(val label: String) { TG("Тоҷикӣ"), RU("Русский"), EN("English") }

enum class ReportType(val tg: String, val ru: String, val en: String) {
    REFERAT("Реферат", "Реферат", "Abstract paper"),
    KMD("КМД", "КМД (самостоятельная работа)", "Independent work (KMD)"),
    INSHO("Иншо", "Сочинение", "Essay"),
    MAQOLA("Мақола", "Статья", "Article"),
    HISOBOT("Ҳисобот", "Отчёт", "Report"),
    KURSI("Кори курсӣ", "Курсовая работа", "Coursework"),
    LOIHA("Лоиҳаи таълимӣ", "Учебный проект", "Educational project");

    fun title(lang: Lang): String = when (lang) {
        Lang.TG -> tg
        Lang.RU -> ru
        Lang.EN -> en
    }
}

/**
 * Сохтори ҳуҷҷат: саҳифаи аввал, нақша, муқаддима, қисми асосӣ, хулоса, адабиёт.
 * Ин генератори сохтор аст (на LLM): матнҳои асосиро истифодабаранда менависад.
 * Байни саҳифаҳо аломати \u000C (page break) гузошта мешавад.
 */
object ReportGenerator {
    const val PAGE_BREAK = '\u000C'

    private class L(
        val plan: String, val intro: String, val main: String, val concl: String, val refs: String,
        val author: String, val sample: String,
        val chapters: List<String>,
        val introText: (String) -> String,
        val chapterText: (String, String) -> String,
        val conclText: (String) -> String
    )

    private val tg = L(
        "Нақша", "Муқаддима", "Қисми асосӣ", "Хулоса", "Рӯйхати адабиёт",
        "Иҷрокунанда", "Намуна",
        listOf(
            "Мафҳум ва моҳияти асосии мавзӯъ",
            "Таҳлили ҷанбаҳои асосӣ ва мисолҳо",
            "Масъалаҳои амалӣ ва роҳҳои ҳалли онҳо"
        ),
        { t -> "Мавзӯи «$t» аҳамияти илмӣ ва амалӣ дорад. Мақсади ин кор — баррасии мавзӯъ ва муайян кардани масъалаҳои асосии он. Барои ноил шудан ба ин мақсад вазифаҳои зерин гузошта шудаанд: омӯзиши мафҳумҳо, таҳлили маводи мавҷуда ва ҷамъбасти натиҷаҳо." },
        { c, t -> "Дар ин қисм «$c» дар заминаи мавзӯи «$t» баррасӣ мегардад." },
        { t -> "Дар натиҷаи баррасии мавзӯи «$t» хулосаҳои асосӣ ҷамъбаст карда шуданд. Ҳар як боб ба ҳалли вазифаҳои гузошташуда мусоидат кард ва натиҷаҳои ба даст омада барои корҳои минбаъда асос шуда метавонанд." }
    )

    private val ru = L(
        "План", "Введение", "Основная часть", "Заключение", "Список литературы",
        "Исполнитель", "Образец",
        listOf(
            "Понятие и основное содержание темы",
            "Анализ основных аспектов и примеры",
            "Практические вопросы и пути их решения"
        ),
        { t -> "Тема «$t» имеет научное и практическое значение. Цель данной работы — рассмотреть тему и определить её основные проблемы. Для достижения цели поставлены задачи: изучить основные понятия, проанализировать имеющиеся материалы и обобщить результаты." },
        { c, t -> "В данном разделе рассматривается «$c» в контексте темы «$t»." },
        { t -> "В результате рассмотрения темы «$t» были обобщены основные выводы. Каждый раздел способствовал решению поставленных задач, а полученные результаты могут служить основой для дальнейших работ." }
    )

    private val en = L(
        "Outline", "Introduction", "Main part", "Conclusion", "References",
        "Author", "Format sample",
        listOf(
            "Concept and core meaning of the topic",
            "Analysis of key aspects and examples",
            "Practical issues and ways to address them"
        ),
        { t -> "The topic \"$t\" has both scientific and practical importance. The aim of this work is to examine the topic and identify its main issues. To reach this aim, the following tasks are set: to study the key concepts, to analyse the available material and to summarise the results." },
        { c, t -> "This section examines \"$c\" in the context of the topic \"$t\"." },
        { t -> "Having examined the topic \"$t\", the main conclusions have been summarised. Each chapter contributed to the tasks that were set, and the results can serve as a basis for further work." }
    )

    private fun labels(lang: Lang) = when (lang) {
        Lang.TG -> tg
        Lang.RU -> ru
        Lang.EN -> en
    }

    fun generate(
        type: ReportType,
        lang: Lang,
        topic: String,
        author: String,
        organization: String
    ): String {
        val l = labels(lang)
        val t = topic.trim().ifEmpty { "—" }
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val sb = StringBuilder()

        // 1. Саҳифаи аввал
        if (organization.isNotBlank()) sb.append(organization.trim()).append("\n\n\n")
        sb.append(type.title(lang).uppercase()).append("\n\n")
        sb.append(if (lang == Lang.EN) "Topic: " else if (lang == Lang.RU) "Тема: " else "Мавзӯъ: ")
            .append("«").append(t).append("»").append("\n\n\n")
        if (author.isNotBlank()) sb.append(l.author).append(": ").append(author.trim()).append("\n\n")
        sb.append(year).append("\n")
        sb.append(PAGE_BREAK)

        // 2. Нақша
        sb.append(l.plan.uppercase()).append("\n\n")
        sb.append("1. ").append(l.intro).append("\n")
        l.chapters.forEachIndexed { i, c -> sb.append("${i + 2}. ").append(c).append("\n") }
        sb.append("${l.chapters.size + 2}. ").append(l.concl).append("\n")
        sb.append("${l.chapters.size + 3}. ").append(l.refs).append("\n")
        sb.append(PAGE_BREAK)

        // 3. Муқаддима
        sb.append(l.intro.uppercase()).append("\n\n").append(l.introText(t)).append("\n\n")

        // 4. Қисми асосӣ
        sb.append(l.main.uppercase()).append("\n\n")
        l.chapters.forEachIndexed { i, c ->
            sb.append("${i + 1}. ").append(c).append("\n")
            sb.append(l.chapterText(c, t)).append("\n\n")
        }

        // 5. Хулоса
        sb.append(l.concl.uppercase()).append("\n\n").append(l.conclText(t)).append("\n")
        sb.append(PAGE_BREAK)

        // 6. Рӯйхати адабиёт
        sb.append(l.refs.uppercase()).append("\n\n")
        sb.append("1. \n2. \n3. \n\n")
        sb.append(l.sample).append(": ")
            .append(if (lang == Lang.EN) "Surname N.N. Title. — City: Publisher, Year."
            else if (lang == Lang.RU) "Фамилия И.О. Название. — Город: Издательство, Год."
            else "Насаб Н.Н. Номи китоб. — Шаҳр: Нашриёт, Сол.")
            .append("\n")
        return sb.toString()
    }
}
