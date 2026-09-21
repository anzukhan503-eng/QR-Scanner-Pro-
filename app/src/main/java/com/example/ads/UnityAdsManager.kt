package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAds.UnityAdsInitializationError
import com.unity3d.ads.UnityAds.UnityAdsLoadError
import com.unity3d.ads.UnityAds.UnityAdsShowCompletionState
import com.unity3d.ads.UnityAds.UnityAdsShowError
import com.unity3d.ads.UnityAdsLoadOptions
import com.unity3d.ads.UnityAdsShowOptions
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UnityAdsManager private constructor() {

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isRewardedLoaded = MutableStateFlow(false)
    val isRewardedLoaded: StateFlow<Boolean> = _isRewardedLoaded.asStateFlow()

    private val _isRewardedLoading = MutableStateFlow(false)
    val isRewardedLoading: StateFlow<Boolean> = _isRewardedLoading.asStateFlow()

    private val _isAdUnavailable = MutableStateFlow(false)
    val isAdUnavailable: StateFlow<Boolean> = _isAdUnavailable.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var activeRewardedPlacement: String = UnityAdsConfig.REWARDED_PLACEMENT_ID

    /**
     * Initialize Unity Ads SDK before loading any ads.
     */
    fun initialize(context: Context) {
        if (UnityAds.isInitialized) {
            Log.d(TAG, "[INIT] Unity Ads is already initialized.")
            _isInitialized.value = true
            loadRewardedAd()
            return
        }

        Log.i(TAG, "[INIT] Initializing Unity Ads SDK with Game ID: ${UnityAdsConfig.GAME_ID}, Test Mode: ${UnityAdsConfig.TEST_MODE}")
        UnityAds.initialize(
            context.applicationContext,
            UnityAdsConfig.GAME_ID,
            UnityAdsConfig.TEST_MODE,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    Log.i(TAG, "[INIT_SUCCESS] Unity Ads initialization completed successfully.")
                    _isInitialized.value = true
                    _lastError.value = null
                    _isAdUnavailable.value = false
                    loadRewardedAd()
                }

                override fun onInitializationFailed(error: UnityAdsInitializationError?, message: String?) {
                    Log.w(TAG, "[INIT_FAILED] Unity Ads initialization notice: error=$error, message=$message")
                    _isInitialized.value = false
                    _isAdUnavailable.value = true
                    _lastError.value = "Ad unavailable. You can continue using the scanner."
                }
            }
        )
    }

    /**
     * Loads the rewarded ad using official Unity Ads lifecycle.
     */
    fun loadRewardedAd(
        onLoaded: (() -> Unit)? = null,
        onFailed: ((String) -> Unit)? = null
    ) {
        if (_isRewardedLoading.value) {
            Log.d(TAG, "[LOAD_INFO] Rewarded ad is already currently loading.")
            return
        }

        if (_isRewardedLoaded.value) {
            Log.d(TAG, "[LOAD_INFO] Rewarded ad is already loaded.")
            onLoaded?.invoke()
            return
        }

        if (!UnityAds.isInitialized) {
            Log.d(TAG, "[LOAD_INFO] Unity Ads not yet initialized. Scanner remains accessible.")
            _lastError.value = "Ad unavailable. You can continue using the scanner."
            _isAdUnavailable.value = true
            onFailed?.invoke("Ad unavailable. You can continue using the scanner.")
            return
        }

        val primaryPlacement = UnityAdsConfig.REWARDED_PLACEMENT_ID
        _isRewardedLoading.value = true
        _lastError.value = null

        loadPlacementInternal(primaryPlacement, isPrimary = true, onLoaded, onFailed)
    }

    private fun loadPlacementInternal(
        placement: String,
        isPrimary: Boolean,
        onLoaded: (() -> Unit)?,
        onFailed: ((String) -> Unit)?
    ) {
        Log.i(TAG, "[LOAD_START] Starting to load rewarded ad for placement: $placement")

        val loadOptions = UnityAdsLoadOptions().apply {
            objectId = UUID.randomUUID().toString()
            if (placement.startsWith("BP_")) {
                // Supply protobuf token to satisfy Header Bidding SDK requirements
                setAdMarkup("EAE=")
            }
        }

        UnityAds.load(
            placement,
            loadOptions,
            object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String?) {
                    val resolvedPlacement = placementId ?: placement
                    Log.i(TAG, "[LOAD_SUCCESS] Rewarded ad ready for placement: $resolvedPlacement")
                    activeRewardedPlacement = resolvedPlacement
                    _isRewardedLoaded.value = true
                    _isRewardedLoading.value = false
                    _isAdUnavailable.value = false
                    _lastError.value = null
                    onLoaded?.invoke()
                }

                override fun onUnityAdsFailedToLoad(
                    placementId: String?,
                    error: UnityAdsLoadError?,
                    message: String?
                ) {
                    Log.i(TAG, "[LOAD_NOTICE] Rewarded ad not available for placement $placementId ($message)")

                    // If primary placement failed and fallback is available, try fallback
                    val fallback = UnityAdsConfig.FALLBACK_REWARDED_PLACEMENT_ID
                    if (isPrimary && fallback.isNotEmpty() && fallback != placement) {
                        Log.i(TAG, "[LOAD_FALLBACK] Attempting fallback placement: $fallback")
                        loadPlacementInternal(fallback, isPrimary = false, onLoaded, onFailed)
                        return
                    }

                    _isRewardedLoaded.value = false
                    _isRewardedLoading.value = false
                    _isAdUnavailable.value = true
                    val friendlyError = "Ad unavailable. You can continue using the scanner."
                    _lastError.value = friendlyError
                    onFailed?.invoke(friendlyError)
                }
            }
        )
    }

    /**
     * Shows the rewarded ad if ready.
     * Grants reward ONLY after verified completion callback (state == COMPLETED).
     */
    fun showRewardedAd(
        activity: Activity,
        onRewardGranted: () -> Unit,
        onFailed: (String) -> Unit
    ) {
        if (!_isRewardedLoaded.value) {
            Log.w(TAG, "[SHOW_WARN] Rewarded ad was not ready when show was requested.")
            val errorMsg = "Ad unavailable. You can continue using the scanner."
            _isAdUnavailable.value = true
            _lastError.value = errorMsg
            onFailed(errorMsg)
            return
        }

        val placement = activeRewardedPlacement
        Log.i(TAG, "[SHOW_START] Showing rewarded ad for placement: $placement")

        UnityAds.show(
            activity,
            placement,
            UnityAdsShowOptions(),
            object : IUnityAdsShowListener {
                override fun onUnityAdsShowStart(placementId: String?) {
                    Log.i(TAG, "[SHOW_STARTED] Rewarded ad started playback for placement: $placementId")
                }

                override fun onUnityAdsShowClick(placementId: String?) {
                    Log.i(TAG, "[SHOW_CLICKED] Rewarded ad clicked for placement: $placementId")
                }

                override fun onUnityAdsShowComplete(
                    placementId: String?,
                    state: UnityAdsShowCompletionState?
                ) {
                    Log.i(TAG, "[SHOW_COMPLETE] Rewarded ad playback completed with state: $state for placement: $placementId")
                    _isRewardedLoaded.value = false

                    if (state == UnityAdsShowCompletionState.COMPLETED) {
                        Log.i(TAG, "[REWARD_GRANTED] User watched entire rewarded ad. Granting scanner access!")
                        onRewardGranted()
                    } else {
                        Log.w(TAG, "[REWARD_NOT_GRANTED] Ad playback was skipped or closed prematurely ($state).")
                        onFailed("Reward requires watching the full advertisement.")
                    }
                }

                override fun onUnityAdsShowFailure(
                    placementId: String?,
                    error: UnityAdsShowError?,
                    message: String?
                ) {
                    Log.w(TAG, "[SHOW_FAILED] Rewarded ad show failed for placement: $placementId | error=$error | message=$message")
                    _isRewardedLoaded.value = false
                    _isAdUnavailable.value = true
                    val errorMsg = "Ad unavailable. You can continue using the scanner."
                    _lastError.value = errorMsg
                    onFailed(errorMsg)
                }
            }
        )
    }

    companion object {
        private const val TAG = "UnityAdsManager"

        @Volatile
        private var instance: UnityAdsManager? = null

        fun getInstance(): UnityAdsManager {
            return instance ?: synchronized(this) {
                instance ?: UnityAdsManager().also { instance = it }
            }
        }
    }
}
