package com.example.core

import android.app.Activity
import com.example.data.model.SubscriptionPlanOffer
import com.example.data.model.WellPlusSubscriptionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Abstract Google Play Billing subscription gateway keeping Play Billing Library v9 decoupled
 * from feature ViewModels and pure-JVM test suites.
 */
interface BillingEntitlementGateway {
    val subscriptionState: StateFlow<WellPlusSubscriptionState>

    fun connectAndRefresh()
    fun launchPurchase(activity: Activity, offer: SubscriptionPlanOffer)
    fun restorePurchases()
    fun disconnect()

    companion object {
        const val WELLPLUS_PRO_MONTHLY_ID = "wellplus_pro_monthly"
        const val WELLPLUS_PRO_ANNUAL_ID = "wellplus_pro_annual"

        val DEFAULT_DISPLAY_CATALOG: List<SubscriptionPlanOffer> = listOf(
            SubscriptionPlanOffer(
                productId = WELLPLUS_PRO_ANNUAL_ID,
                title = "Well+ Pro Annual",
                subtitle = "200 daily AI credits, AI SOAP Pre-Visit PDF briefs & unlimited lab/Rx scans",
                formattedPrice = "$59.99 / yr",
                billingPeriodLabel = "Billed annually",
                badgeText = "Best Value"
            ),
            SubscriptionPlanOffer(
                productId = WELLPLUS_PRO_MONTHLY_ID,
                title = "Well+ Pro Monthly",
                subtitle = "Flexible monthly access to expanded clinical AI & visit prep",
                formattedPrice = "$7.99 / mo",
                billingPeriodLabel = "Billed monthly"
            )
        )
    }
}

class NoOpBillingEntitlementGateway(
    initialState: WellPlusSubscriptionState = WellPlusSubscriptionState(
        offers = BillingEntitlementGateway.DEFAULT_DISPLAY_CATALOG,
        statusMessage = "Google Play Billing is not active in this environment."
    )
) : BillingEntitlementGateway {
    private val _state = MutableStateFlow(initialState)
    override val subscriptionState: StateFlow<WellPlusSubscriptionState> = _state.asStateFlow()

    override fun connectAndRefresh() = Unit
    override fun launchPurchase(activity: Activity, offer: SubscriptionPlanOffer) = Unit
    override fun restorePurchases() = Unit
    override fun disconnect() = Unit
}
