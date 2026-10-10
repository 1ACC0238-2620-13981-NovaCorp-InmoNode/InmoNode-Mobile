package com.novacorp.inmonode_app.features.iam

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** Static resource/source guards; these do not replace Compose instrumentation. */
class LoginUiContractTest {
    @Test fun toggleIsDisabledWhileLoading() {
        val source = File("src/main/java/com/novacorp/inmonode_app/features/iam/presentation/login/LoginScreen.kt").readText()
        assertTrue(source.contains("IconButton(onClick = onTogglePasswordVisibility, enabled = !state.isLoading)"))
    }

    @Test fun editableFieldsHaveExplicitAccessibleNames() {
        val source = File("src/main/java/com/novacorp/inmonode_app/features/iam/presentation/login/LoginScreen.kt").readText()
        assertTrue(source.contains("contentDescription = emailLabel"))
        assertTrue(source.contains("contentDescription = passwordLabel"))
    }

    @Test fun uiCannotRenderTechnicalDetailsOrInventedLockTimer() {
        val source = File("src/main/java/com/novacorp/inmonode_app/features/iam/presentation/login/LoginScreen.kt").readText()
        assertFalse(source.contains("error.message"))
        assertFalse(source.contains("formatCountdown"))
    }
    @Test fun connectionHintDoesNotInventDailyAuthentication() {
        val strings = File("src/main/res/values/strings.xml").readText()
        assertFalse(strings.contains("primer inicio de sesión del día"))
    }
}
