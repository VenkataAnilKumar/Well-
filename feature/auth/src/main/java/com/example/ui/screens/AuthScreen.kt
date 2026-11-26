package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.core.designsystem.R
import com.example.core.AuthUiState
import com.example.core.attemptAutoSignIn
import com.example.core.onGoogleSignInClicked
import com.example.ui.components.SanctuaryErrorBanner
import com.example.ui.theme.GlassHealthCard
import com.example.ui.theme.GlassRenderMode
import com.example.ui.theme.LocalSanaColors
import com.example.ui.theme.SanaTheme

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var authState by remember { mutableStateOf<AuthUiState>(AuthUiState.CheckingSession) }
    var privacyExpanded by remember { mutableStateOf(false) }

    fun launchInteractiveGoogleSignIn() {
        authState = AuthUiState.SigningIn
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                authState = AuthUiState.Idle
                onAuthSuccess()
            },
            onAuthError = { msg ->
                authState = AuthUiState.Error(msg)
            },
            scope = scope,
            onAuthCancelled = {
                authState = AuthUiState.Cancelled
            }
        )
    }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                authState = AuthUiState.Idle
                onAuthSuccess()
            },
            onUnauthenticated = {
                authState = AuthUiState.Idle
            },
            scope = scope
        )
    }

    val sanaColors = LocalSanaColors.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        // Crisp geometric Well+ clinical brand mark + editorial wordmark
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            WellPlusBrandMark(
                                primaryColor = MaterialTheme.colorScheme.primary,
                                accentColor = sanaColors.heartPink,
                                modifier = Modifier.size(56.dp)
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.brand_app_name),
                                    style = MaterialTheme.typography.displayLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = stringResource(R.string.auth_tagline),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Clean, grounded feature list on frosted glass surface
                        GlassHealthCard(
                            cornerRadius = 22.dp,
                            renderMode = GlassRenderMode.REALTIME_BLUR_SURFACE,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                ValueRow(
                                    icon = Icons.Filled.MonitorHeart,
                                    tint = sanaColors.heartPink,
                                    title = "Vitals & Health Connect",
                                    subtitle = "Track BP, glucose, temperature, and wearables in US or Canadian units."
                                )
                                HorizontalDivider(
                                    color = sanaColors.subtleDivider,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                ValueRow(
                                    icon = Icons.Filled.Medication,
                                    tint = MaterialTheme.colorScheme.primary,
                                    title = "Medications & Interactions",
                                    subtitle = "Scan prescription bottles, log daily doses, and check drug pairs."
                                )
                                HorizontalDivider(
                                    color = sanaColors.subtleDivider,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                ValueRow(
                                    icon = Icons.Filled.PictureAsPdf,
                                    tint = sanaColors.sleepIndigo,
                                    title = "Doctor Visit Briefs",
                                    subtitle = "Organize symptom notes, coverage details, and exportable clinical PDFs."
                                )
                            }
                        }

                        AnimatedVisibility(visible = privacyExpanded) {
                            GlassHealthCard(
                                cornerRadius = 20.dp,
                                renderMode = GlassRenderMode.SCROLL_SAFE_FROSTED,
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PrivacyBullet("Passwordless Google Sign-In via Android Credential Manager.")
                                    PrivacyBullet("Records are stored strictly under your private Google account.")
                                    PrivacyBullet("Photos are analyzed in memory and never uploaded to cloud storage.")
                                    HorizontalDivider(
                                        color = sanaColors.subtleDivider,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.auth_disclaimer),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Grounded bottom CTA dock
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (val state = authState) {
                    is AuthUiState.Error -> {
                        SanctuaryErrorBanner(
                            message = state.message,
                            onRetry = { launchInteractiveGoogleSignIn() }
                        )
                    }

                    AuthUiState.Cancelled -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, sanaColors.cardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.auth_cancelled_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    else -> Unit
                }

                GoogleSignInButton(
                    authState = authState,
                    onClick = { launchInteractiveGoogleSignIn() }
                )

                Row(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(CircleShape)
                        .clickable { privacyExpanded = !privacyExpanded }
                        .padding(horizontal = 14.dp)
                        .testTag("auth_privacy_toggle"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (privacyExpanded) {
                            "Hide privacy details"
                        } else {
                            "US & Canada • Adults 18+ • Privacy & Safety"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WellPlusBrandMark(
    primaryColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val label = stringResource(R.string.brand_app_name)
    Canvas(
        modifier = modifier.semantics { contentDescription = label }
    ) {
        val r = size.minDimension * 0.28f
        drawRoundRect(
            color = primaryColor,
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = CornerRadius(r, r)
        )
        val armThickness = size.minDimension * 0.19f
        val armLength = size.minDimension * 0.56f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val armCorner = CornerRadius(armThickness / 2.4f, armThickness / 2.4f)
        // Vertical bar of clinical cross
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(cx - armThickness / 2f, cy - armLength / 2f),
            size = Size(armThickness, armLength),
            cornerRadius = armCorner
        )
        // Horizontal bar of clinical cross
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(cx - armLength / 2f, cy - armThickness / 2f),
            size = Size(armLength, armThickness),
            cornerRadius = armCorner
        )
        // Subtle vitality dot on upper-right corner of the cross
        drawCircle(
            color = accentColor,
            radius = size.minDimension * 0.085f,
            center = Offset(cx + armLength * 0.34f, cy - armLength * 0.34f)
        )
    }
}

@Composable
private fun ValueRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = tint.copy(alpha = 0.12f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .padding(8.dp)
                    .size(18.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GoogleSignInButton(
    authState: AuthUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBusy = authState is AuthUiState.SigningIn || authState is AuthUiState.CheckingSession
    val label = when (authState) {
        AuthUiState.CheckingSession -> stringResource(R.string.auth_checking_session)
        AuthUiState.SigningIn -> stringResource(R.string.auth_signing_in)
        else -> stringResource(R.string.auth_sign_in_google)
    }

    Button(
        onClick = onClick,
        enabled = !isBusy,
        shape = RoundedCornerShape(14.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
            disabledContentColor = Color.White.copy(alpha = 0.9f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("google_sign_in_button")
    ) {
        if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
        } else {
            Surface(
                color = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    GoogleGMark(modifier = Modifier.size(17.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun GoogleGMark(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.cd_google_logo)
    Canvas(
        modifier = modifier.semantics { contentDescription = description }
    ) {
        val strokeWidth = size.minDimension * 0.2f
        val inset = strokeWidth / 2f
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(inset, inset)
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Butt)

        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 215f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 140f,
            sweepAngle = 78f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 45f,
            sweepAngle = 98f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -5f,
            sweepAngle = 55f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
        val centerY = size.height / 2f
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(size.width * 0.52f, centerY),
            end = Offset(size.width - inset, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Butt
        )
    }
}

@Composable
private fun PrivacyBullet(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
