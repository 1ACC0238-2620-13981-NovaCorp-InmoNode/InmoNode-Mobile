package com.novacorp.inmonode_app.features.fieldsales

import com.novacorp.inmonode_app.features.fieldsales.domain.model.ProspectRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProspectRulesTest {

    @Test
    fun documentHasEightToTwelveLettersOrDigits() {
        assertNull(ProspectRules.documentError("70000006"))
        assertNull(ProspectRules.documentError(" CE1234567890 "))
        assertEquals("El documento debe tener de 8 a 12 letras o números.", ProspectRules.documentError(""))
        assertEquals("El documento debe tener de 8 a 12 letras o números.", ProspectRules.documentError("7000-0006"))
    }

    @Test
    fun nameIsRequiredUpToTheMaximum() {
        assertNull(ProspectRules.nameError("Juan Carlos Pérez Rojas"))
        assertEquals("Ingresa el nombre completo (máximo 150 caracteres).", ProspectRules.nameError("   "))
        assertEquals("Ingresa el nombre completo (máximo 150 caracteres).", ProspectRules.nameError("a".repeat(151)))
    }

    @Test
    fun phoneIsOptionalButValidWhenGiven() {
        assertNull(ProspectRules.phoneError(""))
        assertNull(ProspectRules.phoneError("+51 987 654 321"))
        assertEquals("El teléfono debe tener entre 6 y 20 dígitos, espacios o guiones.", ProspectRules.phoneError("987a"))
    }
}
