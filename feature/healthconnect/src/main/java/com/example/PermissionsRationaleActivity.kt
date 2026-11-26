package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.R
import com.example.ui.components.SanctuaryCard
import com.example.ui.theme.LocalSanaColors
import com.example.ui.theme.SanaCareTheme
import com.example.ui.theme.SanctuaryThemePrefs

/**
 * Required Health Connect privacy policy & permission rationale activity.
 * Displayed when the user taps "Privacy policy" from the system Health Connect permission sheet.
 */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val themeMode = SanctuaryThemePrefs.load(this)
        setContent {
            SanaCareTheme(themeMode = themeMode) {
                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    HealthConnectRationaleContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        onClose = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthConnectRationaleContent(
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    val sanaColors = LocalSanaColors.current
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(12.dp)
                    .size(28.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "HEALTH CONNECT PRIVACY POLICY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.health_connect_privacy_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.health_connect_privacy_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SanctuaryCard {
            Text(
                text = "Data Well+ Requests (Read & Write)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            RationaleRow(
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                tint = sanaColors.activityOrange,
                title = "Steps & Daily Activity (Read / Write)",
                body = "Displays your daily movement totals and 7-day activity trends, and lets you log manual step entries back to Health Connect."
            )
            Spacer(modifier = Modifier.height(10.dp))
            RationaleRow(
                icon = Icons.Filled.Favorite,
                tint = sanaColors.heartPink,
                title = "Heart Rate (Read / Write)",
                body = "Shows recent BPM, resting range, and syncs heart rate readings you log in Well+ with Android Health Connect."
            )
            Spacer(modifier = Modifier.height(10.dp))
            RationaleRow(
                icon = Icons.Filled.Bedtime,
                tint = sanaColors.sleepIndigo,
                title = "Sleep Sessions (Read / Write)",
                body = "Summarizes overnight sleep duration and stages, and optionally writes logged sleep duration from your check-ins."
            )
            Spacer(modifier = Modifier.height(10.dp))
            RationaleRow(
                icon = Icons.Filled.MonitorHeart,
                tint = MaterialTheme.colorScheme.primary,
                title = "Blood Pressure (Read / Write)",
                body = "Displays recent systolic/diastolic readings from connected cuffs and saves manually recorded Well+ readings back to Health Connect."
            )
        }

        SanctuaryCard {
            Text(
                text = "How Your Data Is Protected",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "• Well+ only requests Read and Write access for the four categories listed above — you can grant any subset you prefer.\n" +
                    "• Health Connect metrics stay on-device unless you explicitly import them to your private cloud account or request an AI wellness brief.\n" +
                    "• Well+ never sells your health data, never uses it for advertising, and lets you revoke any read or write permission anytime in Android Health Connect settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(
            onClick = onClose,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("hc_rationale_close_button")
        ) {
            Text("Done")
        }
    }
}

@Composable
private fun RationaleRow(
    icon: ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    title: String,
    body: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = tint.copy(alpha = 0.14f),
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
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
