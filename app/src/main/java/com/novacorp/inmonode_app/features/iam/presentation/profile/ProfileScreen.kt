package com.novacorp.inmonode_app.features.iam.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novacorp.inmonode_app.R
import com.novacorp.inmonode_app.core.designsystem.icon.InmoIcons
import com.novacorp.inmonode_app.core.designsystem.theme.InmoNodeAppTheme
import com.novacorp.inmonode_app.features.iam.domain.User
import com.novacorp.inmonode_app.features.iam.domain.UserRole

/** M28 Perfil del agente (US-01). */
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileContent(
        state = state,
        onSignOutClicked = viewModel::onSignOutClicked,
        modifier = modifier
    )

    if (state.isSignOutDialogVisible) {
        AlertDialog(
            onDismissRequest = viewModel::onSignOutDismissed,
            title = { Text(stringResource(R.string.profile_sign_out_dialog_title)) },
            text = { Text(stringResource(R.string.profile_sign_out_dialog_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::signOut) {
                    Text(stringResource(R.string.profile_sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onSignOutDismissed) {
                    Text(stringResource(R.string.profile_sign_out_dialog_cancel))
                }
            }
        )
    }
}

@Composable
fun ProfileContent(
    state: ProfileUiState,
    onSignOutClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Text(
            text = stringResource(R.string.nav_profile),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = InmoIcons.Person,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = state.user?.email.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                state.user?.let { user ->
                    Text(
                        text = stringResource(user.role.labelRes()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Portfolio and sync details (projects, last sync, pending records, catalog version)
        // and "Actualizar portafolio" are added with US-02 and US-11.

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            onClick = onSignOutClicked,
            enabled = !state.isSigningOut,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.profile_sign_out),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

private fun UserRole.labelRes(): Int = when (this) {
    UserRole.FIELD_AGENT -> R.string.role_field_agent
    UserRole.BUYER -> R.string.role_buyer
    UserRole.CATALOG_ADMIN -> R.string.role_catalog_admin
    UserRole.FINANCE_ADMIN -> R.string.role_finance_admin
    UserRole.UNKNOWN -> R.string.role_unknown
}

@Preview(showBackground = true, heightDp = 640)
@Composable
fun ProfileContentPreview() {
    InmoNodeAppTheme {
        ProfileContent(
            state = ProfileUiState(user = User(id = 1, email = "agente@inmobiliaria.com", role = UserRole.FIELD_AGENT)),
            onSignOutClicked = {}
        )
    }
}
