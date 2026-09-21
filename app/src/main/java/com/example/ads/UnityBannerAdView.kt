package com.example.ads

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize

@Composable
fun UnityBannerAdView(
    modifier: Modifier = Modifier,
    placementId: String = UnityAdsConfig.BANNER_PLACEMENT_ID
) {
    val context = LocalContext.current
    val activity = context as? Activity

    if (activity == null) return

    val bannerView = remember {
        BannerView(activity, placementId, UnityBannerSize(320, 50)).apply {
            listener = object : BannerView.IListener {
                override fun onBannerLoaded(bannerAdView: BannerView?) {
                    Log.d("UnityBannerAd", "Banner loaded successfully")
                }

                override fun onBannerFailedToLoad(
                    bannerAdView: BannerView?,
                    errorInfo: BannerErrorInfo?
                ) {
                    Log.w("UnityBannerAd", "Banner failed to load: ${errorInfo?.errorMessage}")
                }

                override fun onBannerClick(bannerAdView: BannerView?) {}
                override fun onBannerLeftApplication(bannerAdView: BannerView?) {}
                override fun onBannerShown(bannerAdView: BannerView?) {}
            }
        }
    }

    DisposableEffect(bannerView) {
        bannerView.load()
        onDispose {
            bannerView.destroy()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { bannerView },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
