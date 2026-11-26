package com.example.feature.widgets

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.core.WidgetSnapshot
import com.example.core.WidgetSnapshotStore

private val PARAM_DIAL_NUMBER = ActionParameters.Key<String>("dial_number")

/**
 * Privacy-first opt-in Emergency Medical ID Home Screen Glance Widget.
 * Remains blank/locked with a prompt until the user explicitly enables
 * "Show on Home Screen Widget" inside Well+ -> Profile -> Medical ID Card,
 * and is automatically wiped on sign-out.
 */
class WellPlusEmergencyIdWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSnapshotStore(context).read()
        provideContent {
            GlanceTheme {
                EmergencyIdWidgetContent(snapshot = snapshot)
            }
        }
    }
}

class WellPlusEmergencyIdWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WellPlusEmergencyIdWidget()
}

@Composable
private fun EmergencyIdWidgetContent(snapshot: WidgetSnapshot) {
    val crimson = ColorProvider(Color(0xFFC62828))
    val white = ColorProvider(Color(0xFFFFFFFF))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(20.dp)
            .padding(12.dp)
    ) {
        // High-contrast crimson emergency banner
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(crimson)
                .cornerRadius(12.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MEDICAL ID",
                style = TextStyle(
                    color = white,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            if (snapshot.emergencyWidgetEnabled && snapshot.emergencyBloodType.isNotBlank()) {
                Text(
                    text = snapshot.emergencyBloodType,
                    style = TextStyle(
                        color = white,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (!snapshot.emergencyWidgetEnabled) {
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .clickable(actionRunCallback<OpenEmergencyIdScreenCallback>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Medical ID Widget Off",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(3.dp))
                Text(
                    text = "Tap to opt in from Profile → Medical ID Card so first responders can see blood type & allergies.",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp
                    ),
                    maxLines = 3
                )
            }
        } else {
            Text(
                text = snapshot.emergencyDisplayName.ifBlank { "Well+ Patient" },
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.height(3.dp))
            Text(
                text = "Allergies: ${snapshot.emergencyAllergiesSummary.ifBlank { "None recorded" }}",
                style = TextStyle(
                    color = crimson,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
            if (snapshot.emergencyMedsSummary.isNotBlank()) {
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = "Meds: ${snapshot.emergencyMedsSummary}",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp
                    ),
                    maxLines = 1
                )
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (snapshot.emergencyContactPhone.isNotBlank()) {
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .background(GlanceTheme.colors.primaryContainer)
                            .cornerRadius(10.dp)
                            .padding(horizontal = 8.dp, vertical = 7.dp)
                            .clickable(
                                actionRunCallback<DialEmergencyCallback>(
                                    actionParametersOf(PARAM_DIAL_NUMBER to snapshot.emergencyContactPhone)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Call ICE",
                            style = TextStyle(
                                color = GlanceTheme.colors.onPrimaryContainer,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = GlanceModifier.width(6.dp))
                }
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .background(crimson)
                        .cornerRadius(10.dp)
                        .padding(horizontal = 8.dp, vertical = 7.dp)
                        .clickable(
                            actionRunCallback<DialEmergencyCallback>(
                                actionParametersOf(PARAM_DIAL_NUMBER to snapshot.emergencyDialNumber)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Call ${snapshot.emergencyDialNumber}",
                        style = TextStyle(
                            color = white,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Opens Android's dialer (`ACTION_DIAL`, zero CALL_PHONE permission required) for 1-tap emergency dialing.
 */
class DialEmergencyCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val number = parameters[PARAM_DIAL_NUMBER].orEmpty().trim().ifBlank { "911" }
        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(dialIntent) }
    }
}

class OpenEmergencyIdScreenCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val openIntent = Intent().setClassName(
            context.packageName,
            "com.example.MainActivity"
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("sana_open_destination", "emergency_id")
        }
        runCatching { context.startActivity(openIntent) }
    }
}
