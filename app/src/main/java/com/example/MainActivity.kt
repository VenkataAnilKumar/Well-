package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.core.AppTelemetryRegistry
import com.example.core.FirebaseCrashlyticsTelemetry
import com.example.core.HealthConnectManager
import com.example.core.SanaNotificationChannels
import com.example.core.authStateFlow
import com.example.core.configureAppCheck
import com.example.feature.widgets.WellPlusWidgetRefresher
import com.example.navigation.DeepLinkParser
import com.example.navigation.PendingDeepLink
import com.example.navigation.WellPlusAuthenticatedWorkspace
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.GlassAmbientBackdrop
import com.example.ui.theme.SanaCareTheme
import com.example.ui.theme.SanctuaryThemeMode
import com.example.ui.theme.SanctuaryThemePrefs
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : FragmentActivity() {
    private val pendingDeepLink = mutableStateOf<PendingDeepLink?>(null)
    private var lastAppliedScreenPrivacy: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent BEFORE UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)
        AppTelemetryRegistry.current = FirebaseCrashlyticsTelemetry()
        WellPlusWidgetRefresher.registerBridge()
        pendingDeepLink.value = DeepLinkParser.fromIntent(intent)

        // Offload NotificationChannel creation and Health Connect client binding off the Main thread
        // so MainActivity reaches its first Compose frame well within SurfaceSyncGroup's 1000ms budget.
        val appCtx = applicationContext
        lifecycleScope.launch(Dispatchers.IO) {
            SanaNotificationChannels.ensureCreated(appCtx)
            HealthConnectManager.getInstance(appCtx).initialize()
        }

        enableEdgeToEdge()
        // Protect app-switcher thumbnail on API 33+ and apply FLAG_SECURE on production physical devices
        // (bypassed on debug/emulator streams so virtual display previews never render blank).
        updateScreenPrivacyIfNeeded(enabled = true)

        setContent {
            var themeMode by remember { mutableStateOf(SanctuaryThemePrefs.load(this@MainActivity)) }

            SanaCareTheme(themeMode = themeMode) {
                GlassAmbientBackdrop {
                    SanaCareRoot(
                        themeMode = themeMode,
                        pendingDeepLink = pendingDeepLink.value,
                        onConsumeDeepLink = { pendingDeepLink.value = null },
                        onThemeModeChange = { nextMode ->
                            themeMode = nextMode
                            SanctuaryThemePrefs.save(this@MainActivity, nextMode)
                        }
                    )
                }
            }
        }
    }

    internal fun updateScreenPrivacyIfNeeded(enabled: Boolean) {
        if (lastAppliedScreenPrivacy == enabled) return
        lastAppliedScreenPrivacy = enabled
        applyScreenPrivacyFlags(this, enabled = enabled)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        DeepLinkParser.fromIntent(intent)?.let { parsed ->
            pendingDeepLink.value = parsed
        }
    }
}

private fun isEmulatorEnvironment(): Boolean {
    val fingerprint = Build.FINGERPRINT.lowercase(Locale.US)
    val model = Build.MODEL.lowercase(Locale.US)
    val product = Build.PRODUCT.lowercase(Locale.US)
    val hardware = Build.HARDWARE.lowercase(Locale.US)
    val manufacturer = Build.MANUFACTURER.lowercase(Locale.US)
    val brand = Build.BRAND.lowercase(Locale.US)
    val device = Build.DEVICE.lowercase(Locale.US)
    return fingerprint.startsWith("generic") ||
        fingerprint.startsWith("unknown") ||
        fingerprint.contains("emulator") ||
        fingerprint.contains("cuttlefish") ||
        fingerprint.contains("vsoc") ||
        model.contains("google_sdk") ||
        model.contains("emulator") ||
        model.contains("android sdk built for") ||
        model.contains("cuttlefish") ||
        manufacturer.contains("genymotion") ||
        hardware.contains("goldfish") ||
        hardware.contains("ranchu") ||
        hardware.contains("cutf") ||
        product.contains("sdk") ||
        product.contains("emulator") ||
        product.contains("cuttlefish") ||
        product.contains("vsoc") ||
        (brand.startsWith("generic") && device.startsWith("generic"))
}

private fun applyScreenPrivacyFlags(activity: FragmentActivity, enabled: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        activity.setRecentsScreenshotEnabled(!enabled)
    }
    val allowHardwareFlagSecure = !BuildConfig.DEBUG && !isEmulatorEnvironment()
    if (enabled && allowHardwareFlagSecure) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    } else {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}

@Composable
private fun SanaCareRoot(
    themeMode: SanctuaryThemeMode,
    pendingDeepLink: PendingDeepLink?,
    onConsumeDeepLink: () -> Unit,
    onThemeModeChange: (SanctuaryThemeMode) -> Unit
) {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = { /* AuthStateListener drives navigation */ })
    } else {
        key(user.uid) {
            WellPlusAuthenticatedWorkspace(
                user = user,
                themeMode = themeMode,
                pendingDeepLink = pendingDeepLink,
                onConsumeDeepLink = onConsumeDeepLink,
                onThemeModeChange = onThemeModeChange
            )
        }
    }
}
