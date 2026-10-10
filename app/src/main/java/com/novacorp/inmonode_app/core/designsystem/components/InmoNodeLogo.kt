package com.novacorp.inmonode_app.core.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.novacorp.inmonode_app.R
import com.novacorp.inmonode_app.core.designsystem.theme.InmoNodeAppTheme

/** Brand mark only (pin over lots). */
@Composable
fun InmoNodeLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Image(
        painter = painterResource(R.drawable.ic_inmonode_logo),
        contentDescription = null,
        modifier = modifier.size(size)
    )
}

/** "Inmo" in the text color and "Node" in the brand green. */
@Composable
fun InmoNodeWordmark(
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineLarge
) {
    Text(
        text = buildAnnotatedString {
            append("Inmo")
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("Node") }
        },
        modifier = modifier,
        style = style.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
    )
}

/** Mark above the wordmark, as in the splash screen. */
@Composable
fun InmoNodeBrand(
    modifier: Modifier = Modifier,
    logoSize: Dp = 96.dp
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        InmoNodeLogo(size = logoSize)
        InmoNodeWordmark()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F8F5)
@Composable
fun InmoNodeBrandPreview() {
    InmoNodeAppTheme {
        InmoNodeBrand()
    }
}
