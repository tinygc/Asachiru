package com.tinygc.asachiru.data.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 広告関連の機能を提供するRepository
 *
 * Google AdMobの初期化と広告の読み込みを管理します。
 */
class AdRepository(private val context: Context) {

    companion object {
        private const val TAG = "AdRepository"
    }

    /**
     * AdMob SDKを初期化します
     *
     * @return 初期化が成功したかどうか
     */
    suspend fun initializeMobileAds(): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            MobileAds.initialize(context, object : OnInitializationCompleteListener {
                override fun onInitializationComplete(initializationStatus: InitializationStatus) {
                    Log.d(TAG, "AdMob initialized: ${initializationStatus.adapterStatusMap}")
                    continuation.resume(true)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AdMob", e)
            continuation.resume(false)
        }
    }

    /**
     * 広告を読み込みます
     *
     * @param adView 広告を表示するAdView
     */
    fun loadAd(adView: AdView) {
        try {
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
            Log.d(TAG, "Ad request sent for AdView: ${adView.adUnitId}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load ad", e)
        }
    }

    /**
     * Android Advertising IDを取得します
     * (将来的にアナリティクスで使う可能性があるため実装)
     *
     * @return Advertising ID (取得失敗時はnull)
     */
    suspend fun getAdvertisingId(): String? = suspendCancellableCoroutine { continuation ->
        try {
            // Advertising ID取得は非同期処理
            // この実装は参考用 (実際の使用時は別途実装が必要)
            continuation.resume(null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Advertising ID", e)
            continuation.resume(null)
        }
    }
}
