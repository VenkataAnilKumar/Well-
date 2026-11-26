package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.PiiCategory
import com.example.core.PiiMatchSummary
import com.example.core.PiiPatternClassifier
import com.example.ui.theme.LocalSanaColors
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val PII_TAG = "WellPlusPiiRedactor"

data class PiiRedactionOutcome(
    val maskedBitmap: Bitmap,
    val originalBitmap: Bitmap,
    val summaries: List<PiiMatchSummary>,
    val redactedBoxCount: Int,
    val ocrSucceeded: Boolean
)

/**
 * On-device ML Kit Text Recognition v2 PII detector & bitmap pixel-masking engine.
 * Normalizes immutable/hardware bitmaps into software ARGB_8888 copies on `Dispatchers.Default`
 * so sensitive identifiers (SSN, MRN, phone, email, DOB) are stripped locally before upload.
 */
object OnDevicePiiRedactor {

    suspend fun redactBitmap(source: Bitmap): PiiRedactionOutcome = withContext(Dispatchers.Default) {
        val softwareCopy = ensureSoftwareMutableCopy(source)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val visionText = recognizer.processSuspend(InputImage.fromBitmap(softwareCopy, 0))
            val matchedBoxes = mutableListOf<Pair<android.graphics.Rect, Set<PiiCategory>>>()

            visionText.textBlocks.forEach { block ->
                block.lines.forEach { line ->
                    val categories = PiiPatternClassifier.detectCategories(line.text)
                    val box = line.boundingBox
                    if (categories.isNotEmpty() && box != null) {
                        matchedBoxes.add(box to categories)
                    }
                }
            }

            if (matchedBoxes.isNotEmpty()) {
                val canvas = Canvas(softwareCopy)
                val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(18, 22, 28)
                    style = Paint.Style.FILL
                }
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(66, 133, 244)
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                matchedBoxes.forEach { (rect, _) ->
                    val pad = 6f
                    val expanded = RectF(
                        (rect.left - pad).coerceAtLeast(0f),
                        (rect.top - pad).coerceAtLeast(0f),
                        (rect.right + pad).coerceAtMost(softwareCopy.width.toFloat()),
                        (rect.bottom + pad).coerceAtMost(softwareCopy.height.toFloat())
                    )
                    canvas.drawRoundRect(expanded, 8f, 8f, fillPaint)
                    canvas.drawRoundRect(expanded, 8f, 8f, borderPaint)
                }
            }

            PiiRedactionOutcome(
                maskedBitmap = softwareCopy,
                originalBitmap = source,
                summaries = PiiPatternClassifier.summarizeCategories(matchedBoxes.map { it.second }),
                redactedBoxCount = matchedBoxes.size,
                ocrSucceeded = true
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(PII_TAG, "On-device ML Kit OCR unavailable or failed; returning unmasked bitmap", e)
            PiiRedactionOutcome(
                maskedBitmap = softwareCopy,
                originalBitmap = source,
                summaries = emptyList(),
                redactedBoxCount = 0,
                ocrSucceeded = false
            )
        } finally {
            runCatching { recognizer.close() }
        }
    }

    private fun ensureSoftwareMutableCopy(bitmap: Bitmap): Bitmap {
        val copied = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        if (copied != null) return copied
        val fallback = Bitmap.createBitmap(
            bitmap.width.coerceAtLeast(1),
            bitmap.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        Canvas(fallback).drawBitmap(bitmap, 0f, 0f, null)
        return fallback
    }

    private suspend fun com.google.mlkit.vision.text.TextRecognizer.processSuspend(
        image: InputImage
    ): Text = suspendCancellableCoroutine { cont ->
        process(image)
            .addOnSuccessListener { result ->
                if (cont.isActive) cont.resume(result)
            }
            .addOnFailureListener { error ->
                if (cont.isActive) cont.resumeWithException(error)
            }
    }
}

/**
 * Shared Privacy Redaction Preview bottom sheet invoked before lab report, prescription bottle,
 * or visual consult photo uploads so users can visually confirm local PII masking.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PiiRedactionPreviewSheet(
    sourceBitmap: Bitmap,
    scanPurposeTitle: String,
    confirmButtonLabel: String = "Scan Redacted Photo",
    onDismiss: () -> Unit,
    onConfirmUpload: (Bitmap) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sanaColors = LocalSanaColors.current
    var outcome by remember(sourceBitmap) { mutableStateOf<PiiRedactionOutcome?>(null) }
    var useMaskedBitmap by remember(sourceBitmap) { mutableStateOf(true) }

    LaunchedEffect(sourceBitmap) {
        outcome = OnDevicePiiRedactor.redactBitmap(sourceBitmap)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("pii_redaction_preview_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = "On-device privacy shield",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp).size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "On-Device Privacy Preview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = scanPurposeTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val currentOutcome = outcome
            if (currentOutcome == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Text(
                            text = "Scanning photo locally for personal IDs…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val displayBitmap = if (useMaskedBitmap) {
                    currentOutcome.maskedBitmap
                } else {
                    currentOutcome.originalBitmap
                }
                Image(
                    bitmap = displayBitmap.asImageBitmap(),
                    contentDescription = if (useMaskedBitmap) {
                        "Redacted photo preview"
                    } else {
                        "Original photo preview"
                    },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (currentOutcome.redactedBoxCount > 0) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    } else {
                        sanaColors.healthyGreen.copy(alpha = 0.14f)
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (currentOutcome.redactedBoxCount > 0) {
                                    Icons.Rounded.VisibilityOff
                                } else {
                                    Icons.Rounded.CheckCircle
                                },
                                contentDescription = null,
                                tint = if (currentOutcome.redactedBoxCount > 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    sanaColors.healthyGreen
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = when {
                                    currentOutcome.redactedBoxCount > 0 ->
                                        "${currentOutcome.redactedBoxCount} personal identifier(s) auto-masked on-device"
                                    currentOutcome.ocrSucceeded ->
                                        "No SSN, MRN, phone, email, or DOB text detected locally"
                                    else ->
                                        "Local OCR unavailable — review photo before uploading"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (currentOutcome.summaries.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                currentOutcome.summaries.forEach { summary ->
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                    ) {
                                        Text(
                                            text = "${summary.category.displayLabel} (${summary.count})",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (currentOutcome.redactedBoxCount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Keep PII Redaction Masks Applied",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (useMaskedBitmap) {
                                    "Recommended — masked areas never leave your device."
                                } else {
                                    "Unmasked preview active — personal IDs will be visible to AI."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (useMaskedBitmap) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = useMaskedBitmap,
                            onCheckedChange = { useMaskedBitmap = it },
                            modifier = Modifier.testTag("pii_mask_toggle_switch")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("pii_preview_cancel_button")
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onConfirmUpload(
                                if (useMaskedBitmap) currentOutcome.maskedBitmap
                                else currentOutcome.originalBitmap
                            )
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .minimumInteractiveComponentSize()
                            .testTag("pii_preview_confirm_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PrivacyTip,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (useMaskedBitmap && currentOutcome.redactedBoxCount > 0) {
                                confirmButtonLabel
                            } else {
                                "Scan Photo"
                            }
                        )
                    }
                }
            }
        }
    }
}
