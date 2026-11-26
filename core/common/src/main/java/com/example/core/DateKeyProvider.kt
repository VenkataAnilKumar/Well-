package com.example.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Midnight-safe reactive provider for local ISO date keys (`yyyy-MM-dd`).
 * Injectable [clockMillis] allows deterministic JVM unit testing across midnight rollovers.
 */
class DateKeyProvider(
    private val clockMillis: () -> Long = { System.currentTimeMillis() }
) {
    private val _todayDateKeyFlow = MutableStateFlow(computeTodayKey())
    val todayDateKeyFlow: StateFlow<String> = _todayDateKeyFlow.asStateFlow()

    fun todayKey(): String {
        val current = computeTodayKey()
        if (_todayDateKeyFlow.value != current) {
            _todayDateKeyFlow.value = current
        }
        return current
    }

    fun refresh(): Boolean {
        val current = computeTodayKey()
        val changed = _todayDateKeyFlow.value != current
        if (changed) {
            _todayDateKeyFlow.value = current
        }
        return changed
    }

    private fun computeTodayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(clockMillis()))

    companion object {
        fun formatCurrentDateKey(timeMillis: Long = System.currentTimeMillis()): String =
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timeMillis))
    }
}
