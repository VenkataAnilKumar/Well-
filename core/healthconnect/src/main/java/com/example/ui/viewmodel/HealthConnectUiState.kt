package com.example.ui.viewmodel

import com.example.core.HealthConnectManager
import com.example.data.model.HealthConnectAvailability
import com.example.data.model.HealthConnectSnapshot

data class HealthConnectUiState(
    val availability: HealthConnectAvailability = HealthConnectAvailability.NOT_SUPPORTED,
    val isClientInitialized: Boolean = false,
    val frameworkSourceLabel: String = "",
    val grantedPermissions: Set<String> = emptySet(),
    val isSyncing: Boolean = false,
    val snapshot: HealthConnectSnapshot? = null,
    val errorMessage: String? = null
) {
    val hasAnyGrantedPermission: Boolean
        get() = grantedPermissions.intersect(HealthConnectManager.allPermissions).isNotEmpty()

    val hasAnyReadPermission: Boolean
        get() = grantedPermissions.intersect(HealthConnectManager.readPermissions).isNotEmpty()

    val hasAnyWritePermission: Boolean
        get() = grantedPermissions.intersect(HealthConnectManager.writePermissions).isNotEmpty()

    val hasAllRequiredPermissions: Boolean
        get() = grantedPermissions.containsAll(HealthConnectManager.allPermissions)

    val missingPermissions: Set<String>
        get() = HealthConnectManager.allPermissions - grantedPermissions
}
