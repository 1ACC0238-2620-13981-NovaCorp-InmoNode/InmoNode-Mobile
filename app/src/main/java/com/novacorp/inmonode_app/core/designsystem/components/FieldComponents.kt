package com.novacorp.inmonode_app.core.designsystem.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.novacorp.inmonode_app.R

/** Shared mobile form rhythm from the approved 360 x 800 frames. */
@Composable
fun FieldPage(title: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null,
    actions: @Composable ColumnScope.() -> Unit = {}, topActions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxSize().imePadding()) {
        // A page without "back" is a top-level step (M04, M23): its large title sits on the page, like the tab screens.
        Surface(color = if (onBack == null) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                if (onBack != null) IconButton(onClick = onBack) {
                    Icon(painter = androidx.compose.ui.res.painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.field_back))
                }
                Text(title, style = if (onBack == null) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(8.dp).weight(1f))
                topActions()
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp), content = actions)
        }
    }
}
@Composable
fun FieldError(message: String?) {
    if (message != null) Surface(color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}
/** Exposed dropdown menu (style guide, M06, M13): a read-only outlined field with an arrow that lists the options. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FieldDropdown(label: String, value: String, options: List<T>, optionLabel: (T) -> String,
    onSelect: (T) -> Unit, modifier: Modifier = Modifier, supportingText: String? = null) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(value = value, onValueChange = {}, readOnly = true, singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            supportingText = supportingText?.let { text -> { Text(text) } },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable))
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(optionLabel(option)) }, onClick = { onSelect(option); expanded = false },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding)
            }
        }
    }
}
/** Segmented button (style guide, M12, M29): switches between sibling lists and shows which one is open. */
@Composable
fun SegmentedTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            SegmentedButton(selected = index == selected, onClick = { if (index != selected) onSelect(index) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = labels.size), label = { Text(label) })
        }
    }
}
@Composable
fun FieldValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.End)
    }
}
