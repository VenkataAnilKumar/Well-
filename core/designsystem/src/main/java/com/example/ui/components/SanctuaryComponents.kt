package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Emergency
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MedicalInformation
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.R
import com.example.data.model.UrgencyLevel
import com.example.ui.theme.GlassHealthCard
import com.example.ui.theme.GlassRenderMode
import com.example.ui.theme.LocalSanaColors
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SanctuaryCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassHealthCard(
        modifier = modifier,
        containerColor = containerColor.takeIf { it != Color.Unspecified },
        borderColor = borderColor.takeIf { it != Color.Unspecified },
        renderMode = GlassRenderMode.SCROLL_SAFE_FROSTED,
        content = content
    )
}

@Composable
fun UrgencyBadge(
    urgencyLevel: UrgencyLevel,
    modifier: Modifier = Modifier
) {
    val sanaColors = LocalSanaColors.current
    val (fg, bg, icon) = when (urgencyLevel) {
        UrgencyLevel.SELF_CARE -> Triple(sanaColors.selfCareFg, sanaColors.selfCareBg, Icons.Rounded.CheckCircle)
        UrgencyLevel.DOCTOR_SOON -> Triple(sanaColors.doctorSoonFg, sanaColors.doctorSoonBg, Icons.Rounded.Schedule)
        UrgencyLevel.URGENT -> Triple(sanaColors.urgentFg, sanaColors.urgentBg, Icons.Rounded.WarningAmber)
        UrgencyLevel.EMERGENCY -> Triple(sanaColors.emergencyFg, sanaColors.emergencyBg, Icons.Rounded.Emergency)
    }

    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = urgencyLevel.label,
                tint = fg,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = urgencyLevel.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = fg
            )
        }
    }
}

@Composable
fun EmergencyAlertBanner(
    urgencyLevel: UrgencyLevel,
    modifier: Modifier = Modifier
) {
    if (urgencyLevel != UrgencyLevel.EMERGENCY && urgencyLevel != UrgencyLevel.URGENT) return
    val sanaColors = LocalSanaColors.current
    val isEmergency = urgencyLevel == UrgencyLevel.EMERGENCY
    val fg = if (isEmergency) sanaColors.emergencyFg else sanaColors.urgentFg
    val bg = if (isEmergency) sanaColors.emergencyBg else sanaColors.urgentBg

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = bg,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, fg.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (isEmergency) Icons.Rounded.Emergency else Icons.Rounded.WarningAmber,
                contentDescription = urgencyLevel.label,
                tint = fg,
                modifier = Modifier.size(24.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isEmergency) {
                        "Possible Medical Emergency"
                    } else {
                        "Prompt Medical Evaluation Recommended"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = fg
                )
                Text(
                    text = if (isEmergency) {
                        "Your symptoms may require immediate in-person emergency care. Please call local emergency services or go to the nearest emergency department now."
                    } else {
                        "These symptoms warrant same-day medical attention from a clinician or urgent care clinic. Do not delay care if symptoms worsen."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun MedicalDisclaimerNote(
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.disclaimer_short)
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            modifier = Modifier
                .padding(top = 1.dp)
                .size(15.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
        )
    }
}

@Composable
fun SanctuaryErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            if (onRetry != null) {
                TextButton(
                    onClick = onRetry,
                    modifier = Modifier.testTag("error_retry_button")
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
fun EmptySanctuaryState(
    icon: ImageVector = Icons.Rounded.MedicalInformation,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    actionTestTag: String = "empty_action_button",
    onAction: (() -> Unit)? = null,
    illustrationKind: HealthEmptyIllustrationKind? = null,
    modifier: Modifier = Modifier
) {
    SanctuaryCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (illustrationKind != null) {
                GlassEmptyStateIllustration(kind = illustrationKind)
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .padding(14.dp)
                            .size(28.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier.testTag(actionTestTag),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditableTagSection(
    title: String,
    placeholder: String,
    items: List<String>,
    quickSuggestions: List<String>,
    testTagPrefix: String,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val trimmed = draft.trim()
                        if (trimmed.isNotEmpty()) {
                            onAdd(trimmed)
                            draft = ""
                        }
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("${testTagPrefix}_input")
            )
            FilledTonalIconButton(
                onClick = {
                    val trimmed = draft.trim()
                    if (trimmed.isNotEmpty()) {
                        onAdd(trimmed)
                        draft = ""
                    }
                },
                modifier = Modifier
                    .size(52.dp)
                    .testTag("${testTagPrefix}_add_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.cd_add_item)
                )
            }
        }

        if (items.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items.forEach { tag ->
                    InputChip(
                        selected = true,
                        onClick = { onRemove(tag) },
                        label = { Text(tag, style = MaterialTheme.typography.labelMedium) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Remove $tag",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        val availableSuggestions = quickSuggestions.filter { suggestion ->
            items.none { it.equals(suggestion, ignoreCase = true) }
        }
        if (availableSuggestions.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                availableSuggestions.forEach { suggestion ->
                    SuggestionChip(
                        onClick = { onAdd(suggestion) },
                        label = { Text("+ $suggestion", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
    }
}

fun formatTimestamp(timestamp: Timestamp?): String {
    val date = (timestamp ?: Timestamp.now()).toDate()
    val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return formatter.format(date)
}
