package com.novacorp.inmonode_app.features.iam

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.novacorp.inmonode_app.core.designsystem.theme.InmoNodeAppTheme
import com.novacorp.inmonode_app.features.iam.presentation.login.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Prepared instrumentation: compilation alone does not certify runtime results. */
@RunWith(AndroidJUnit4::class)
class LoginContentTest {
    @get:Rule val compose = createComposeRule()

    private fun render(state: LoginUiState, toggle: () -> Unit = {}) {
        compose.setContent {
            InmoNodeAppTheme {
                LoginContent(state, {}, {}, toggle, {})
            }
        }
    }

    @Test fun passwordIsHiddenAndToggleHasAccessibleDescription() {
        render(LoginUiState(email = "agent@example.test", password = "synthetic-password"))
        compose.onNodeWithContentDescription("Correo electrónico").assertExists().assert(hasSetTextAction())
        compose.onNodeWithContentDescription("Contraseña").assertExists().assert(hasSetTextAction())
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password)).assertExists()
        compose.onNodeWithContentDescription("Mostrar contraseña").assertExists().assertHasClickAction()
    }

    @Test fun visiblePasswordToggleOffersHideAction() {
        render(LoginUiState(password = "synthetic-password", isPasswordHidden = false))
        compose.onNodeWithContentDescription("Ocultar contraseña").assertExists()
    }

    @Test fun loadingDisablesEditableFieldsAndSubmit() {
        render(LoginUiState(email = "agent@example.test", password = "synthetic-password", isLoading = true))
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(2)
        compose.onAllNodes(hasSetTextAction())[0].assertIsNotEnabled()
        compose.onAllNodes(hasSetTextAction())[1].assertIsNotEnabled()
        compose.onNodeWithContentDescription("Mostrar contraseña").assertIsNotEnabled()
        val buttons = compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Button))
        buttons.assertCountEquals(2)
        buttons[0].assertIsNotEnabled()
        buttons[1].assertIsNotEnabled()
        compose.onNodeWithText("Iniciar sesión").assertDoesNotExist()
    }

    @Test fun unknownErrorDoesNotRenderTechnicalMessage() {
        render(LoginUiState(error = LoginError.Unknown("synthetic-sensitive-detail")))
        compose.onNodeWithText("Ocurrió un error inesperado. Inténtalo de nuevo.").assertExists()
        compose.onNodeWithText("synthetic-sensitive-detail", substring = true).assertDoesNotExist()
    }

    @Test fun accountLockDoesNotDisplayInventedCountdown() {
        render(LoginUiState(error = LoginError.AccountLocked))
        compose.onNodeWithText("El servidor no informa el tiempo restante", substring = true).assertExists()
        compose.onNodeWithText("Iniciar sesión").assertExists()
    }
}
