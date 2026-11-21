package com.novacorp.inmonode_app.features.fieldsales.domain.model

/** Field rules of a prospect, shared by the form (errors under each field) and the repository (last check before saving). */
object ProspectRules {
    const val MAX_NAME_LENGTH = 150
    private val DOCUMENT = Regex("[0-9A-Za-z]{8,12}")
    private val PHONE = Regex("""\+?[0-9 -]{6,20}""")

    fun documentError(document: String): String? =
        if (document.trim().matches(DOCUMENT)) null else "El documento debe tener de 8 a 12 letras o números."

    fun nameError(fullName: String): String? =
        if (fullName.trim().length in 1..MAX_NAME_LENGTH) null else "Ingresa el nombre completo (máximo $MAX_NAME_LENGTH caracteres)."

    fun phoneError(phone: String): String? =
        if (phone.isBlank() || phone.trim().matches(PHONE)) null else "El teléfono debe tener entre 6 y 20 dígitos, espacios o guiones."
}
