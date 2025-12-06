package com.tinygc.asachiru.data.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
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
        private var isInitialized = false
    }

    /**
     * AdMob SDKを初期化します
     *
     * @return 初期化が成功したかどうか
     */
    suspend fun initializeMobileAds(): Boolean = suspendCancellableCoroutine { continuation ->
        // 既に初期化済みの場合はスキップ
        if (isInitialized) {
            Log.d(TAG, "AdMob already initialized, skipping")
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }
        
        try {
            // テストデバイスIDを設定（開発中は必ずテスト広告を表示）
            val requestConfiguration = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                .build()
            MobileAds.setRequestConfiguration(requestConfiguration)
            
            MobileAds.initialize(context, object : OnInitializationCompleteListener {
                override fun onInitializationComplete(initializationStatus: InitializationStatus) {
                    Log.d(TAG, "AdMob initialized: ${initializationStatus.adapterStatusMap}")
                    isInitialized = true
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
            
            // 広告読み込み状況をログ出力（デバッグ用）
            adView.adListener = object : com.google.android.gms.ads.AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "✅ Ad loaded successfully")
                }
                
                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    Log.e(TAG, "❌ Ad failed to load: ${error.message}")
                    Log.e(TAG, "Error code: ${error.code}")
                    Log.e(TAG, "Error domain: ${error.domain}")
                    Log.e(TAG, "Error cause: ${error.cause}")
                    // エラーコード3 = 広告在庫なし
                    // エラーコード0 = 内部エラー
                    // エラーコード1 = ネットワークエラー
                }
                
                override fun onAdOpened() {
                    Log.d(TAG, "Ad opened")
                }
                
                override fun onAdClosed() {
                    Log.d(TAG, "Ad closed")
                }
            }
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
