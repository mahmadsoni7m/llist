package tj.soni.listoffice.domain

/** Офлайн абзорҳои матнӣ: хулоса, ҷустуҷӯ, тозакунӣ. Бе сервер ва бе API. */
object TextTools {

    fun summarize(text: String, maxSentences: Int = 3): String {
        val sentences = text.replace('\u000C', '\n')
            .split(Regex("(?<=[.!?])\\s+|\\n+"))
            .map { it.trim() }
            .filter { it.length >= 25 }
        if (sentences.size <= maxSentences) return sentences.joinToString(" ")
        val wordRe = Regex("\\p{L}{4,}")
        val freq = HashMap<String, Int>()
        for (s in sentences) {
            for (m in wordRe.findAll(s.lowercase())) {
                freq[m.value] = (freq[m.value] ?: 0) + 1
            }
        }
        val scored = sentences.mapIndexed { i, s ->
            val ws = wordRe.findAll(s.lowercase()).map { it.value }.toList()
            val score = if (ws.isEmpty()) 0.0 else ws.sumOf { (freq[it] ?: 0).toDouble() } / ws.size
            Triple(i, s, score)
        }
        return scored.sortedByDescending { it.third }
            .take(maxSentences)
            .sortedBy { it.first }
            .joinToString(" ") { it.second }
    }

    fun countMatches(text: String, query: String, ignoreCase: Boolean = true): Int {
        if (query.isEmpty()) return 0
        var count = 0
        var from = 0
        while (true) {
            val idx = text.indexOf(query, from, ignoreCase)
            if (idx < 0) break
            count++
            from = idx + query.length
        }
        return count
    }

    fun wordCount(text: String): Int = text.split(Regex("\\s+")).count { it.isNotBlank() }

    fun cleanSpaces(text: String): String =
        text.lines()
            .joinToString("\n") { it.trim().replace(Regex("[ \\t]{2,}"), " ") }
            .replace(Regex("\\n{3,}"), "\n\n")
}
