package com.novacorp.inmonode_app.features.iam.presentation.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novacorp.inmonode_app.BuildConfig
import com.novacorp.inmonode_app.R
import com.novacorp.inmonode_app.core.designsystem.components.InmoNodeLogo
import com.novacorp.inmonode_app.core.designsystem.components.MessageCard
import com.novacorp.inmonode_app.core.designsystem.components.MessageType
import com.novacorp.inmonode_app.core.designsystem.icon.InmoIcons
import com.novacorp.inmonode_app.core.designsystem.theme.InmoNodeAppTheme

/** M02 Iniciar sesión and M03 Acceso bloqueado (US-01). */
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) onLoginSuccess()
    }

    LoginContent(
        state = state,
        onEmailChanged = viewModel::onEmailChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
        onSignIn = viewModel::signIn,
        modifier = modifier
    )
}

@Composable
fun LoginContent(
    state: LoginUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCredentialError = state.error == LoginError.InvalidCredentials || state.error == LoginError.AccountLocked
    val emailLabel = stringResource(R.string.login_email_label)
    val passwordLabel = stringResource(R.string.login_password_label)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        InmoNodeLogo(size = 48.dp)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val alert = when {
            state.retryAfterSecondsRemaining > 0 ->
                stringResource(R.string.login_retry_after, state.retryAfterSecondsRemaining)
            else -> state.error?.takeIf { it != LoginError.InvalidCredentials }?.let { loginErrorMessage(it) }
        }
        if (alert != null) {
            Spacer(modifier = Modifier.height(16.dp))
            MessageCard(text = alert, type = MessageType.ERROR)
        }

        Spacer(modifier = Modifier.height(24.dp))
        FieldLabel(text = emailLabel)
        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChanged,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = emailLabel },
            placeholder = { Text(stringResource(R.string.login_email_placeholder)) },
            singleLine = true,
            enabled = !state.isLoading,
            shape = FieldShape,
            colors = fieldColors(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(16.dp))
        FieldLabel(text = passwordLabel)
        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChanged,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = passwordLabel },
            singleLine = true,
            enabled = !state.isLoading,
            isError = isCredentialError,
            shape = FieldShape,
            colors = fieldColors(),
            visualTransformation = if (state.isPasswordHidden) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSignIn() }),
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility, enabled = !state.isLoading) {
                    Icon(
                        imageVector = if (state.isPasswordHidden) InmoIcons.Visibility else InmoIcons.VisibilityOff,
                        contentDescription = stringResource(
                            if (state.isPasswordHidden) R.string.login_show_password else R.string.login_hide_password
                        )
                    )
                }
            },
            supportingText = if (state.error == LoginError.InvalidCredentials) {
                { Text(stringResource(R.string.login_error_invalid_credentials)) }
            } else {
                null
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onSignIn,
            enabled = state.canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = FieldShape
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = stringResource(R.string.login_button),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        if (state.isLoading) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.login_connecting),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        MessageCard(text = stringResource(R.string.login_connection_hint))

        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = stringResource(R.string.login_footer, BuildConfig.VERSION_NAME),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val FieldShape = RoundedCornerShape(8.dp)

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surface,
    errorContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
)

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        // The associated input owns the accessible name; avoid a duplicate label stop.
        modifier = Modifier.padding(bottom = 8.dp).clearAndSetSemantics {},
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun loginErrorMessage(error: LoginError): String = when (error) {
    LoginError.InvalidCredentials -> stringResource(R.string.login_error_invalid_credentials)
    LoginError.AccountLocked -> stringResource(R.string.login_error_locked_unknown)
    LoginError.AccountInactive -> stringResource(R.string.login_error_inactive)
    LoginError.NotFieldAgent -> stringResource(R.string.login_error_not_field_agent)
    LoginError.TooManyRequests -> stringResource(R.string.login_error_too_many_requests)
    LoginError.Network -> stringResource(R.string.login_error_network)
    is LoginError.Unknown -> stringResource(R.string.login_error_unknown)
}

@Preview(showBackground = true, heightDp = 800)
@Composable
fun LoginContentPreview() {
    InmoNodeAppTheme {
        LoginContent(
            state = LoginUiState(email = "agente@inmobiliaria.com", password = "secret"),
            onEmailChanged = {},
            onPasswordChanged = {},
            onTogglePasswordVisibility = {},
            onSignIn = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
fun LoginContentLockedPreview() {
    InmoNodeAppTheme {
        LoginContent(
            state = LoginUiState(
                email = "agente@inmobiliaria.com",
                password = "secret",
                error = LoginError.AccountLocked
            ),
            onEmailChanged = {},
            onPasswordChanged = {},
            onTogglePasswordVisibility = {},
            onSignIn = {}
        )
    }
}
