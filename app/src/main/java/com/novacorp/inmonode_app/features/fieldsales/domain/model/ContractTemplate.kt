package com.novacorp.inmonode_app.features.fieldsales.domain.model

/**
 * The preliminary contract template (US-13): a title and sections whose text holds `{{variables}}`.
 *
 * Text format: the first line is `version: <v>`, `# ` starts the title and `## ` each section; the lines below a
 * section are its text, and a blank line starts a new paragraph.
 */
data class ContractTemplate(val version: String, val title: String, val sections: List<Section>) {
    data class Section(val heading: String, val body: String)

    /** Every variable the template uses. */
    val variables: Set<String>
        get() = (listOf(title) + sections.flatMap { listOf(it.heading, it.body) })
            .flatMap { text -> PLACEHOLDER.findAll(text).map { it.groupValues[1] } }.toSet()

    /** The template with each variable replaced; fails when one has no value, so no clause is left with a blank. */
    fun fill(values: Map<String, String>): ContractTemplate {
        val unknown = variables - values.keys
        require(unknown.isEmpty()) { "La plantilla del contrato usa datos desconocidos: ${unknown.sorted().joinToString()}." }
        fun String.filled() = PLACEHOLDER.replace(this) { values.getValue(it.groupValues[1]) }
        return copy(title = title.filled(), sections = sections.map { Section(it.heading.filled(), it.body.filled()) })
    }

    companion object {
        private val PLACEHOLDER = Regex("""\{\{\s*([A-Za-z.]+)\s*}}""")

        fun parse(text: String): ContractTemplate {
            val lines = text.lines()
            val version = lines.firstOrNull()?.takeIf { it.startsWith("version:") }?.substringAfter(':')?.trim()
            require(!version.isNullOrEmpty()) { "La plantilla del contrato no indica su versión." }
            var title: String? = null
            val sections = mutableListOf<Section>()
            var heading: String? = null
            val body = mutableListOf<String>()
            fun close() {
                heading?.let { sections += Section(it, paragraphs(body)) }
                body.clear()
            }
            for (line in lines.drop(1)) when {
                line.startsWith("## ") -> { close(); heading = line.removePrefix("## ").trim() }
                line.startsWith("# ") -> title = line.removePrefix("# ").trim()
                heading != null -> body += line.trim()
            }
            close()
            require(!title.isNullOrEmpty() && sections.isNotEmpty()) { "La plantilla del contrato no tiene título o cláusulas." }
            return ContractTemplate(version, title, sections)
        }

        /** Joins the lines of each paragraph; paragraphs stay separated by a blank line. */
        private fun paragraphs(lines: List<String>): String = lines.joinToString("\n").trim()
            .split(Regex("""\n\s*\n""")).joinToString("\n\n") { it.lines().joinToString(" ") }
    }
}
