package com.example.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.core.designsystem.R

/**
 * Strongly-typed route descriptors for the Navigation Compose graph.
 * Uses Kotlin sealed objects mapped to stable route templates without requiring extra compiler plugins.
 */
sealed interface WellPlusRoute {
    val route: String

    data object Today : WellPlusRoute {
        override val route: String = "today"
    }

    data object Meds : WellPlusRoute {
        override val route: String = "meds"
    }

    data object Consult : WellPlusRoute {
        override val route: String = "consult"
    }

    data object History : WellPlusRoute {
        override val route: String = "history"
    }

    data object Profile : WellPlusRoute {
        override val route: String = "profile"
    }

    data object DailyCheckIn : WellPlusRoute {
        override val route: String = "checkin"
    }

    data object Vitals : WellPlusRoute {
        override val route: String = "vitals"
    }

    data object MentalWellbeing : WellPlusRoute {
        override val route: String = "mental"
    }

    data object BodyAndCycle : WellPlusRoute {
        override val route: String = "bodycycle"
    }

    data object HealthConnect : WellPlusRoute {
        override val route: String = "healthconnect"
    }

    data object WellPlusPro : WellPlusRoute {
        override val route: String = "pro"
    }

    data object EmergencyMedicalId : WellPlusRoute {
        override val route: String = "emergency_id"
    }

    companion object {
        private val topLevelRoutes: Set<String> = setOf(
            Today.route,
            Meds.route,
            Consult.route,
            History.route,
            Profile.route
        )

        fun isTopLevelTab(route: String?): Boolean = route != null && route in topLevelRoutes
    }
}

enum class WellPlusTab(
    val routeId: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    TODAY(WellPlusRoute.Today.route, R.string.nav_today, Icons.Rounded.Home, Icons.Outlined.Home),
    MEDS(WellPlusRoute.Meds.route, R.string.nav_meds, Icons.Rounded.Medication, Icons.Outlined.Medication),
    CONSULT(WellPlusRoute.Consult.route, R.string.nav_consult, Icons.Rounded.MedicalServices, Icons.Outlined.MedicalServices),
    HISTORY(WellPlusRoute.History.route, R.string.nav_history, Icons.Rounded.History, Icons.Outlined.History),
    PROFILE(WellPlusRoute.Profile.route, R.string.nav_profile, Icons.Rounded.Person, Icons.Outlined.Person)
}
