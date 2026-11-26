package com.example.core

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.data.model.SubscriptionPlanOffer
import com.example.data.model.WellPlusSubscriptionState
import com.example.data.model.WellPlusTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

private const val TAG_BILLING = "WellPlusPlayBilling"

/**
 * Production Google Play Billing Library v9 (`billing-ktx:9.1.0`) subscription manager.
 *
 * Manages connection lifecycle, SKU catalog loading (`wellplus_pro_annual`, `wellplus_pro_monthly`),
 * real subscription purchase flows, automatic purchase acknowledgment, and active entitlement
 * restoration via `queryPurchasesAsync(ProductType.SUBS)`.
 */
class PlayBillingManager(
    appContext: Context
) : BillingEntitlementGateway, PurchasesUpdatedListener {

    private val _subscriptionState = MutableStateFlow(
        WellPlusSubscriptionState(
            tier = WellPlusTier.FREE,
            offers = BillingEntitlementGateway.DEFAULT_DISPLAY_CATALOG
        )
    )
    override val subscriptionState: StateFlow<WellPlusSubscriptionState> = _subscriptionState.asStateFlow()

    private val productDetailsCache = ConcurrentHashMap<String, ProductDetails>()

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    override fun connectAndRefresh() {
        if (billingClient.isReady) {
            querySubscriptionCatalog()
            queryActiveSubscriptions(showRestoreMessage = false)
            return
        }
        try {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        querySubscriptionCatalog()
                        queryActiveSubscriptions(showRestoreMessage = false)
                    } else {
                        Log.w(TAG_BILLING, "Billing setup returned ${billingResult.responseCode}: ${billingResult.debugMessage}")
                        _subscriptionState.update {
                            it.copy(
                                storeAvailable = false,
                                isProcessing = false,
                                offers = it.offers.ifEmpty { BillingEntitlementGateway.DEFAULT_DISPLAY_CATALOG }
                            )
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Log.w(TAG_BILLING, "Google Play Billing service disconnected; auto-reconnection enabled.")
                }
            })
        } catch (e: Exception) {
            Log.w(TAG_BILLING, "Google Play Billing unavailable on this device: ${e.message}")
            _subscriptionState.update {
                it.copy(
                    storeAvailable = false,
                    isProcessing = false,
                    offers = it.offers.ifEmpty { BillingEntitlementGateway.DEFAULT_DISPLAY_CATALOG }
                )
            }
        }
    }

    override fun launchPurchase(activity: Activity, offer: SubscriptionPlanOffer) {
        if (!billingClient.isReady) {
            _subscriptionState.update {
                it.copy(
                    statusMessage = "Google Play Billing is not connected right now. Reconnecting..."
                )
            }
            connectAndRefresh()
            return
        }

        val details = productDetailsCache[offer.productId]
        val offerToken = offer.offerToken.ifBlank {
            details?.subscriptionOfferDetails?.firstOrNull()?.offerToken.orEmpty()
        }

        if (details == null || offerToken.isBlank()) {
            _subscriptionState.update {
                it.copy(
                    storeAvailable = false,
                    statusMessage = "This subscription SKU (${offer.productId}) is not published on the current Google Play track yet. All core Well+ features remain available on Free."
                )
            }
            return
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        _subscriptionState.update { it.copy(isProcessing = true, statusMessage = null) }
        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _subscriptionState.update {
                it.copy(
                    isProcessing = false,
                    statusMessage = "Could not open Google Play purchase sheet (${result.debugMessage.ifBlank { "code ${result.responseCode}" }})."
                )
            }
        }
    }

    override fun restorePurchases() {
        _subscriptionState.update { it.copy(isProcessing = true, statusMessage = null) }
        if (!billingClient.isReady) {
            connectAndRefresh()
            _subscriptionState.update {
                it.copy(
                    isProcessing = false,
                    statusMessage = "Google Play Store is not reachable on this device right now."
                )
            }
            return
        }
        queryActiveSubscriptions(showRestoreMessage = true)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (!purchases.isNullOrEmpty()) {
                    handlePurchases(purchases, showRestoreMessage = false, fromPurchaseCallback = true)
                } else {
                    _subscriptionState.update { it.copy(isProcessing = false) }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _subscriptionState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = "Purchase canceled. Your current plan was not changed."
                    )
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                queryActiveSubscriptions(showRestoreMessage = true)
            }
            else -> {
                Log.w(TAG_BILLING, "Purchase error ${billingResult.responseCode}: ${billingResult.debugMessage}")
                _subscriptionState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = "Google Play could not complete the purchase (${billingResult.debugMessage.ifBlank { "code ${billingResult.responseCode}" }})."
                    )
                }
            }
        }
    }

    override fun disconnect() {
        runCatching { billingClient.endConnection() }
    }

    private fun querySubscriptionCatalog() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingEntitlementGateway.WELLPLUS_PRO_ANNUAL_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingEntitlementGateway.WELLPLUS_PRO_MONTHLY_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG_BILLING, "SKU query failed: ${billingResult.responseCode} ${billingResult.debugMessage}")
                _subscriptionState.update {
                    it.copy(
                        storeAvailable = false,
                        offers = it.offers.ifEmpty { BillingEntitlementGateway.DEFAULT_DISPLAY_CATALOG }
                    )
                }
                return@queryProductDetailsAsync
            }

            val detailsList = queryProductDetailsResult.productDetailsList
            if (detailsList.isEmpty()) {
                _subscriptionState.update {
                    it.copy(
                        storeAvailable = false,
                        offers = BillingEntitlementGateway.DEFAULT_DISPLAY_CATALOG
                    )
                }
                return@queryProductDetailsAsync
            }

            productDetailsCache.clear()
            detailsList.forEach { productDetailsCache[it.productId] = it }

            val mappedOffers = detailsList
                .sortedBy { if (it.productId == BillingEntitlementGateway.WELLPLUS_PRO_ANNUAL_ID) 0 else 1 }
                .map { details -> mapProductDetailsToOffer(details) }

            _subscriptionState.update {
                it.copy(
                    storeAvailable = true,
                    offers = mappedOffers
                )
            }
        }
    }

    private fun queryActiveSubscriptions(showRestoreMessage: Boolean) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                _subscriptionState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = if (showRestoreMessage) {
                            "Could not check Google Play subscriptions right now."
                        } else {
                            it.statusMessage
                        }
                    )
                }
                return@queryPurchasesAsync
            }
            handlePurchases(
                purchases = purchases,
                showRestoreMessage = showRestoreMessage,
                fromPurchaseCallback = false
            )
        }
    }

    private fun handlePurchases(
        purchases: List<Purchase>,
        showRestoreMessage: Boolean,
        fromPurchaseCallback: Boolean
    ) {
        val validProIds = setOf(
            BillingEntitlementGateway.WELLPLUS_PRO_ANNUAL_ID,
            BillingEntitlementGateway.WELLPLUS_PRO_MONTHLY_ID
        )
        val activeProPurchase = purchases.firstOrNull { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                purchase.products.any { it in validProIds }
        }
        val hasPending = purchases.any { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PENDING &&
                purchase.products.any { it in validProIds }
        }

        if (activeProPurchase != null) {
            if (!activeProPurchase.isAcknowledged) {
                val ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(activeProPurchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(ackParams) { ackResult ->
                    if (ackResult.responseCode != BillingClient.BillingResponseCode.OK) {
                        Log.w(TAG_BILLING, "Acknowledge purchase failed: ${ackResult.debugMessage}")
                    }
                }
            }
            val matchedProductId = activeProPurchase.products.firstOrNull { it in validProIds }
            _subscriptionState.update {
                it.copy(
                    tier = WellPlusTier.PRO,
                    activeProductId = matchedProductId,
                    isProcessing = false,
                    statusMessage = when {
                        fromPurchaseCallback -> "Welcome to Well+ Pro! Your daily AI quota is now 200 requests/day."
                        showRestoreMessage -> "Well+ Pro subscription restored."
                        else -> it.statusMessage
                    }
                )
            }
        } else if (hasPending) {
            _subscriptionState.update {
                it.copy(
                    isProcessing = false,
                    statusMessage = "Your Well+ Pro purchase is pending confirmation with Google Play."
                )
            }
        } else {
            _subscriptionState.update {
                it.copy(
                    tier = WellPlusTier.FREE,
                    activeProductId = null,
                    isProcessing = false,
                    statusMessage = if (showRestoreMessage) {
                        "No active Well+ Pro subscription was found on this Google Play account."
                    } else {
                        it.statusMessage
                    }
                )
            }
        }
    }

    private fun mapProductDetailsToOffer(details: ProductDetails): SubscriptionPlanOffer {
        val offerDetails = details.subscriptionOfferDetails?.firstOrNull()
        val pricingPhase = offerDetails?.pricingPhases?.pricingPhaseList?.lastOrNull()
        val isAnnual = details.productId == BillingEntitlementGateway.WELLPLUS_PRO_ANNUAL_ID

        val formattedPrice = pricingPhase?.formattedPrice
            ?: if (isAnnual) "$59.99 / yr" else "$7.99 / mo"
        val periodLabel = when (pricingPhase?.billingPeriod) {
            "P1Y" -> "Billed annually"
            "P1M" -> "Billed monthly"
            "P1W" -> "Billed weekly"
            else -> if (isAnnual) "Billed annually" else "Billed monthly"
        }

        return SubscriptionPlanOffer(
            productId = details.productId,
            title = details.name.ifBlank { if (isAnnual) "Well+ Pro Annual" else "Well+ Pro Monthly" },
            subtitle = details.description.ifBlank {
                if (isAnnual) {
                    "200 daily AI credits, AI SOAP Pre-Visit PDF briefs & unlimited lab/Rx scans"
                } else {
                    "Flexible monthly access to expanded clinical AI & visit prep"
                }
            },
            formattedPrice = formattedPrice,
            billingPeriodLabel = periodLabel,
            offerToken = offerDetails?.offerToken.orEmpty(),
            badgeText = if (isAnnual) "Best Value" else ""
        )
    }
}
