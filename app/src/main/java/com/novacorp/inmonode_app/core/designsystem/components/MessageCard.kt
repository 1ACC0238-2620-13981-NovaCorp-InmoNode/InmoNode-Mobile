package com.novacorp.inmonode_app.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.novacorp.inmonode_app.core.designsystem.icon.InmoIcons
import com.novacorp.inmonode_app.core.designsystem.theme.InmoNodeAppTheme

enum class MessageType { INFO, ERROR }

/** Inline message with an icon: info hints and error alerts (e.g. M02, M03, M05, M25, M26). */
@Composable
fun MessageCard(
    text: String,
    modifier: Modifier = Modifier,
    type: MessageType = MessageType.INFO
) {
    val colors = MaterialTheme.colorScheme
    val containerColor: Color
    val contentColor: Color
    val iconColor: Color
    val icon: ImageVector
    when (type) {
        MessageType.INFO -> {
            containerColor = colors.primaryContainer.copy(alpha = 0.5f)
            contentColor = colors.onSurfaceVariant
            iconColor = colors.primary
            icon = InmoIcons.Info
        }
        MessageType.ERROR -> {
            containerColor = colors.errorContainer
            contentColor = colors.onSurface
            iconColor = colors.error
            icon = InmoIcons.Error
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = if (type == MessageType.ERROR) BorderStroke(1.dp, colors.error) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = iconColor
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (type == MessageType.ERROR) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F8F5)
@Composable
fun MessageCardPreview() {
    InmoNodeAppTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MessageCard(text = "Se requiere conexión a internet para el primer inicio de sesión del día.")
            MessageCard(text = "Acceso bloqueado temporalmente.", type = MessageType.ERROR)
        }
    }
}
