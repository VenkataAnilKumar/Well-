package com.example.core

import com.example.data.model.WellPlusTier

/**
 * Seam decoupling feature ViewModels from local fair-use quota tracking vs.
 * Firebase Remote Config + Google Play Billing Well+ Pro entitlements.
 */
interface AiEntitlementProvider {
    val cloudAiGloballyEnabled: Boolean
    val dailyLimit: Int
    val urgentSafetyNotice: String?
    val isProTier: Boolean get() = false
    val labBiomarkerAiEnabled: Boolean get() = true
    val clarifyingQuestionsEnabled: Boolean get() = true
    val proactiveReassessmentEnabled: Boolean get() = true

    fun computeRemaining(settings: SanctuaryDeviceSettings): Int
    suspend fun tryConsumeDailyQuota(): AiQuotaCheckResult
    fun fingerprintOf(vararg parts: Any?): String
    fun <T : Any> getCached(cacheKey: String, fingerprint: String): T?
    fun <T : Any> putCached(cacheKey: String, fingerprint: String, value: T)
    fun clearCache()
}

class LocalAiEntitlementProvider(
    private val quotaAndCacheManager: AiQuotaAndCacheManager,
    private val subscriptionTierProvider: () -> WellPlusTier = { WellPlusTier.FREE }
) : AiEntitlementProvider {

    override val cloudAiGloballyEnabled: Boolean
        get() = quotaAndCacheManager.featureFlags.cloudAiGloballyEnabled

    override val isProTier: Boolean
        get() = subscriptionTierProvider() == WellPlusTier.PRO

    override val dailyLimit: Int
        get() {
            val baseLimit = quotaAndCacheManager.featureFlags.dailyAiFairUseLimit
            return if (isProTier) maxOf(baseLimit, WellPlusTier.PRO.dailyAiQuota) else baseLimit
        }

    override val urgentSafetyNotice: String?
        get() = quotaAndCacheManager.featureFlags.urgentSafetyNotice

    override val labBiomarkerAiEnabled: Boolean
        get() = quotaAndCacheManager.featureFlags.labBiomarkerAiEnabled

    override val clarifyingQuestionsEnabled: Boolean
        get() = quotaAndCacheManager.featureFlags.clarifyingQuestionsEnabled

    override val proactiveReassessmentEnabled: Boolean
        get() = quotaAndCacheManager.featureFlags.proactiveReassessmentEnabled

    override fun computeRemaining(settings: SanctuaryDeviceSettings): Int =
        quotaAndCacheManager.computeRemaining(settings, overrideDailyLimit = dailyLimit)

    override suspend fun tryConsumeDailyQuota(): AiQuotaCheckResult =
        quotaAndCacheManager.tryConsumeDailyQuota(overrideDailyLimit = dailyLimit)

    override fun fingerprintOf(vararg parts: Any?): String =
        quotaAndCacheManager.fingerprintOf(*parts)

    override fun <T : Any> getCached(cacheKey: String, fingerprint: String): T? =
        quotaAndCacheManager.getCached(cacheKey, fingerprint)

    override fun <T : Any> putCached(cacheKey: String, fingerprint: String, value: T) {
        quotaAndCacheManager.putCached(cacheKey, fingerprint, value)
    }

    override fun clearCache() {
        quotaAndCacheManager.clearCache()
    }
}
