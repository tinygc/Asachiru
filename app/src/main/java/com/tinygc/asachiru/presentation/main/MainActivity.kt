package com.tinygc.asachiru.presentation.main

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import android.content.res.Configuration
import com.tinygc.asachiru.presentation.setup.SetupActivity
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import com.tinygc.asachiru.BuildConfig
import com.tinygc.asachiru.R
import com.tinygc.asachiru.databinding.ActivityMainBinding
import com.tinygc.asachiru.domain.util.DeviceUtils
import com.tinygc.asachiru.domain.util.QrCodeGenerator
import com.tinygc.asachiru.presentation.common.ViewModelFactory
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * メイン画面のActivity
 *
 * 全てのCustom Viewを配置し、MainViewModelの状態を監視して
 * UIを更新します。
 *
 * TODO: ViewModelFactoryを実装して、依存性注入を行う
 */
class MainActivity : AppCompatActivity() {

    // ViewBinding: activity_main.xmlに合わせて再生成されたbindingを使う
    private lateinit var binding: com.tinygc.asachiru.databinding.ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var viewModelFactory: com.tinygc.asachiru.presentation.common.ViewModelFactory
    // MusicPlayerへのアクセス（Visualizer用 - RepositoryFactoryグローバルシングルトン）
    private val musicPlayer by lazy { viewModelFactory.let { com.tinygc.asachiru.di.RepositoryFactory(applicationContext).getMusicPlayer() } }
    private var lastVisualizerSessionId: Int = -1
    private val audioPermission = android.Manifest.permission.RECORD_AUDIO
    private val requestCodeAudio = 1001
    private var isNavigatingToSetup = false
    
    // NewsViewの元のレイアウトパラメータを保存（詳細表示から戻る時にXMLの設定を復元するため）
    private var originalNewsViewLayoutParams: androidx.constraintlayout.widget.ConstraintLayout.LayoutParams? = null
    // スマホでのフリック検出用GestureDetector
    private lateinit var gestureDetector: GestureDetector
    
    // AdMob用Repository（シングルトンとして保持）
    private lateinit var adRepository: com.tinygc.asachiru.data.repository.AdRepository
    private var isAdMobInitialized = false
    
    // 動的に作成するAdView（アダプティブバナー対応）
    private var adView: AdView? = null
    
    // TV版スマホ誘導用QRコード（キャッシュ）
    private var qrBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isTelevision()) {
            enableEdgeToEdge()
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Edge-to-edge設定とイベントリスナー
        applyEdgeToEdgeAndWindowInsets()
        setupEventListeners()

        // ViewModelFactoryを生成して保持
        viewModelFactory = ViewModelFactory(applicationContext)
        // ViewModelFactoryを使用してViewModelを生成
        viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]
        
        // AdMob Repository初期化（実際の初期化はonResumeで行う）
        adRepository = com.tinygc.asachiru.data.repository.AdRepository(applicationContext)
        
        // AdViewのサイズを設定（observeViewModelより前に実行）
        setupAdView()
        
        // TV用QRコードを事前生成（非同期）
        if (isTelevision()) {
            preGenerateQrCode()
        }

        observeViewModel()
        // Asachiru: 音声ビジュアライザー用に音声録音権限が必要
        // FeedWatch: ビジュアライザーがないため不要
        if (BuildConfig.FLAVOR == "asachiru") {
            checkAudioPermission()
        }
    }
    
    /**
     * TV用QRコードを事前生成（起動時の非同期処理）
     */
    private fun preGenerateQrCode() {
        lifecycleScope.launch {
            val playStoreUrl = when (BuildConfig.FLAVOR) {
                "asachiru" -> QrCodeGenerator.PlayStoreUrls.ASACHIRU
                "feedwatch" -> QrCodeGenerator.PlayStoreUrls.FEEDWATCH
                else -> QrCodeGenerator.PlayStoreUrls.ASACHIRU
            }
            qrBitmap = QrCodeGenerator.generateQrCodeAsync(playStoreUrl, 200)
            android.util.Log.d("QrPromotion", "QRコード事前生成完了: $playStoreUrl")
        }
    }

    /**
     * イベントリスナーを設定
     */
    private fun setupEventListeners() {
        // スマホ用フリック検出のGestureDetector初期化
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                // スマホのみフリック検出を有効化
                if (!DeviceUtils.isPhone(applicationContext)) {
                    return false
                }

                val diffX = e2.x - (e1?.x ?: 0f)
                val diffY = e2.y - (e1?.y ?: 0f)

                // 横方向のフリックかどうか判定（横移動が縦移動の2倍以上）
                if (abs(diffX) > abs(diffY) * 2 && abs(diffX) > 100) {
                    // 左右フリックでTTS切り替え
                    viewModel.toggleTts()
                    return true
                }
                
                // 縦方向のフリックかどうか判定（縦移動が横移動の2倍以上）
                // スマホはSNS風操作: 上スワイプ=次、下スワイプ=前（コンテンツを押し上げる感覚）
                if (abs(diffY) > abs(diffX) * 2 && abs(diffY) > 100) {
                    if (diffY < 0) {
                        // 上フリック: 次のニュースへ（TikTok/Instagram風）
                        viewModel.navigateToNextNews()
                    } else {
                        // 下フリック: 前のニュースへ
                        viewModel.navigateToPreviousNews()
                    }
                    return true
                }
                
                return false
            }

            // シングルタップで詳細表示切り替え（スマホのみ）
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (DeviceUtils.isPhone(applicationContext)) {
                    // QRコードエリアをタップした場合はブラウザで記事を開く
                    if (binding.newsView.isQrCodeAreaTapped(e.x, e.y)) {
                        binding.newsView.getCurrentNewsUrl()?.let { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            startActivity(intent)
                        }
                        return true
                    }
                    
                    // それ以外のエリアは詳細表示切り替え
                    val state = viewModel.stateMachine.state.value
                    if (state is NewsReadingState.ReadingArticle) {
                        viewModel.toggleNewsDetail()
                    }
                    return true
                }
                return false
            }
        })

        // 設定ボタンの表示制御とクリックリスナー（スマホのみ表示）
        if (DeviceUtils.isPhone(applicationContext)) {
            // スマホ: 設定ボタンを表示（キー操作ヒントは非表示）
            binding.settingsButton.visibility = android.view.View.VISIBLE
            binding.keyHintView?.visibility = android.view.View.GONE
            // スマホではfocusableInTouchModeを無効化（1タップで即反応させる）
            binding.settingsButton.isFocusableInTouchMode = false
            binding.settingsButton.setOnClickListener {
                navigateToSetup()
            }
        } else {
            // TV: キー操作ヒントを表示し、設定ボタンは非表示
            binding.settingsButton.visibility = android.view.View.GONE
            binding.keyHintView?.visibility = android.view.View.VISIBLE
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 回転時に新しいレイアウトをインフレートして適用
        // Androidは自動的にlayout/またはlayout-land/を選択する
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // レイアウト設定を再初期化
        applyEdgeToEdgeAndWindowInsets()
        setupEventListeners()
        
        // CustomViewを強制的に再描画して、サイズ変更を反映
        binding.clockView.requestLayout()
        binding.weatherView.requestLayout()
        binding.newsView.requestLayout()
    }

    /**
     * Edge-to-edgeとWindowInsetsを設定
     */
    private fun applyEdgeToEdgeAndWindowInsets() {
        if (!isTelevision()) {
            // Edge-to-edge: 上端のビュー（時計）、下端のビュー（広告・設定ボタン）にinsetsを適用
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                // 時計ビューに上部insetを適用
                binding.clockView.updatePadding(
                    top = systemBars.top
                )
                // 広告コンテナに下部insetを適用
                binding.adViewContainer.updatePadding(
                    bottom = systemBars.bottom
                )
                // 設定ボタンに下部insetを適用（ナビゲーションバー対応）
                binding.settingsButton.updatePadding(
                    bottom = systemBars.bottom
                )
                WindowInsetsCompat.CONSUMED
            }
        }

        // 画面を常にオンに保つ（アンビエントモード防止）
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /**
     * TVデバイスかどうかを判定
     *
     * `UiModeManager.currentModeType` は一部のOEM端末でスマートフォンでも
     * TVモードと誤って報告されることがあるため、Leanback機能の有無も合わせて
     * 判定する [DeviceUtils.isStrictTelevision] に委譲する。
     * これにより、誤判定によって `enableEdgeToEdge()` がスキップされ、
     * 一部のユーザーでエッジ ツー エッジ表示が有効にならない不具合を防ぐ。
     */
    private fun isTelevision(): Boolean {
        return DeviceUtils.isStrictTelevision(applicationContext)
    }

    /**
     * ViewModelの状態を監視してUIを更新
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // デバッグ情報の更新（DEBUG BUILDのみ）
                    updateDebugInfo(state)
                    
                    // 時計の更新
                    binding.clockView.updateDateTime(state.dateTime)

                    // 天気の更新
                    if (state.weatherError != null) {
                        binding.weatherView.showError(state.weatherError)
                    } else {
                        binding.weatherView.updateWeather(state.weather)
                    }

                    // ニュースの更新
                           // 上下矢印の表示制御
                        val isFirst = state.currentArticleIndex <= 0
                        val isLast = state.currentArticleIndex >= state.totalArticles - 1
                        binding.arrowUp.visibility = if (isFirst) android.view.View.GONE else android.view.View.VISIBLE
                        binding.arrowDown.visibility = if (isLast) android.view.View.GONE else android.view.View.VISIBLE
                    if (state.newsError != null) {
                        binding.newsView.showError(state.newsError)
                    } else {
                        binding.newsView.updateNews(state.currentNews)
                        binding.newsView.setShowDetail(state.showNewsDetail)
                        binding.newsView.setEnableTts(state.enableTts)
                        binding.newsView.setIsSpeaking(state.isSpeaking)
                        binding.newsView.setProgress(state.newsProgressPercent)
                        
                        // 新着待ちアナウンスの表示制御（左下のテキスト）
                        binding.waitingAnnouncementView?.visibility = 
                            if (state.showWaitingAnnouncement) android.view.View.VISIBLE else android.view.View.GONE
                        
                        // 「次へ」ラベルの表示制御（TTS OFF かつ進行中のみ表示）
                        val showNextLabel = !state.enableTts && state.newsProgressPercent > 0f
                        binding.progressNextLabel?.visibility = if (showNextLabel) android.view.View.VISIBLE else android.view.View.GONE
                        
                        // 詳細表示時はNewsViewを全画面表示にする
                        updateNewsViewLayout(state.showNewsDetail)
                    }

                    // 音楽トラック情報の更新
                    binding.musicTrackView.updateMusic(state.currentTrack)

                    // 広告の表示/非表示制御
                    updateAdView(state.showAd, state.adRemainingSeconds)

                    // Asachiru: ビジュアライザー表示制御（BGM OFF時は非表示）
                    // FeedWatch: ビジュアライザーなしため常に非表示
                    if (BuildConfig.FLAVOR == "asachiru") {
                        // BGM OFF時はビジュアライザーと曲情報を非表示
                        if (!state.enableBgm) {
                            binding.visualizerView.visibility = android.view.View.GONE
                            binding.musicTrackView.visibility = android.view.View.GONE
                            if (lastVisualizerSessionId != -1) {
                                binding.visualizerView.stopVisualizer()
                                lastVisualizerSessionId = -1
                            }
                        } else {
                            binding.visualizerView.visibility = android.view.View.VISIBLE
                            binding.musicTrackView.visibility = android.view.View.VISIBLE

                            // ビジュアライザー起動ロジック
                            val sessionId = musicPlayer.getAudioSessionId()
                            val isPlaying = musicPlayer.isPlaying()
                            if (state.currentTrack != null && hasAudioPermission()) {
                                if (sessionId != 0 && isPlaying && sessionId != lastVisualizerSessionId) {
                                    android.util.Log.d("Visualizer", "startVisualizer audioSessionId=$sessionId track=${state.currentTrack.title} isPlaying=$isPlaying")
                                    binding.visualizerView.startVisualizer(sessionId)
                                    lastVisualizerSessionId = sessionId
                                    // 1秒後に状態確認
                                    binding.visualizerView.postDelayed({
                                        android.util.Log.d("Visualizer", "Status check: isFallback=${binding.visualizerView.isUsingFallback()} sessionId=$sessionId")
                                    }, 1000)
                                } else if (sessionId == 0) {
                                    android.util.Log.d("Visualizer", "audioSessionId=0 (MediaPlayer未初期化) currentTrack=${state.currentTrack.title} isPlaying=$isPlaying")
                                } else if (!isPlaying) {
                                    android.util.Log.d("Visualizer", "音楽が再生されていないためVisualizerスキップ sessionId=$sessionId track=${state.currentTrack.title}")
                                }
                            } else if (lastVisualizerSessionId != -1 && state.currentTrack == null) {
                                android.util.Log.d("Visualizer", "stopVisualizer lastSession=$lastVisualizerSessionId")
                                binding.visualizerView.stopVisualizer()
                                lastVisualizerSessionId = -1
                            } else if (state.currentTrack != null && !hasAudioPermission()) {
                                android.util.Log.d("Visualizer", "RECORD_AUDIO未許可のためVisualizer起動スキップ")
                            }
                        }
                    } else {
                        // FeedWatch: ビジュアライザーなし
                        binding.visualizerView.visibility = android.view.View.GONE
                        binding.musicTrackView.visibility = android.view.View.GONE
                    }
                }
            }
        }
    }
    
    /**
     * NewsViewのレイアウトを詳細表示状態に応じて切り替え
     * 通常時はXMLで定義されたレイアウトを使用、詳細表示時は全画面表示
     */
    private fun updateNewsViewLayout(isDetailShown: Boolean) {
        val currentLayoutParams = binding.newsView.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        
        if (isDetailShown) {
            // 初回のみ元のレイアウトパラメータを保存（コピーを作成）
            if (originalNewsViewLayoutParams == null) {
                originalNewsViewLayoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(currentLayoutParams)
            }
            
            // 詳細表示時: 全画面表示
            currentLayoutParams.width = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT
            currentLayoutParams.height = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT
            currentLayoutParams.setMargins(0, 0, 0, 0)
            
            // 最前面に表示
            binding.newsView.bringToFront()
            // 親Viewの再描画を強制
            (binding.newsView.parent as? android.view.ViewGroup)?.invalidate()
            
            binding.newsView.layoutParams = currentLayoutParams
            binding.newsView.requestLayout()
        } else {
            // 通常時: XMLで定義されたレイアウトを復元
            originalNewsViewLayoutParams?.let { original ->
                binding.newsView.layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(original)
                binding.newsView.requestLayout()
                // 復元後にクリア（次回詳細表示時に再度保存するため）
                originalNewsViewLayoutParams = null
            }
        }
    }

    /**
     * リモコンキーイベントのハンドリング
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                // 決定キー: 長押し検出のためトラッキング開始
                event?.startTracking()
                true
            }
            KeyEvent.KEYCODE_BACK -> {
                // 戻るキー: 詳細表示中なら閉じる、通常時はデフォルト動作
                if (viewModel.uiState.value.showNewsDetail) {
                    viewModel.closeNewsDetail()
                    true
                } else {
                    super.onKeyDown(keyCode, event)
                }
            }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                // 左右キー: TTS切り替え
                viewModel.toggleTts()
                true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                // 上キー: 前のニュースへ
                viewModel.navigateToPreviousNews()
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                // 下キー: 次のニュースへ
                viewModel.navigateToNextNews()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    /**
     * 決定キー短押し: 詳細表示切り替え
     */
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                // 長押しでなければ短押し処理（詳細表示切り替え）
                if (event?.isTracking == true && event.isLongPress.not()) {
                    val state = viewModel.stateMachine.state.value
                    if (state is NewsReadingState.ReadingArticle) {
                        viewModel.toggleNewsDetail()
                    }
                }
                true
            }
            else -> super.onKeyUp(keyCode, event)
        }
    }

    /**
     * 決定キー長押し: 初期設定画面へ遷移
     */
    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                // 初期設定画面へ遷移（アプリ終了処理をスキップするためフラグを立てる）
                isNavigatingToSetup = true
                val intent = Intent(this, SetupActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onKeyLongPress(keyCode, event)
        }
    }

    /**
     * 設定画面へ遷移（タッチ操作対応）
     */
    private fun navigateToSetup() {
        // 初期設定画面へ遷移（アプリ終了処理をスキップするためフラグを立てる）
        isNavigatingToSetup = true
        val intent = Intent(this, SetupActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }
    
    /**
     * ウィンドウフォーカス変更時のコールバック
     * 画面が完全に表示された後にAdMobを初期化（UIフリーズ回避）
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !isAdMobInitialized) {
            // 画面がフォーカスを持った後にAdMob初期化（1秒遅延でUI描画完了を待つ）
            lifecycleScope.launch {
                kotlinx.coroutines.delay(1000)
                val initialized = adRepository.initializeMobileAds()
                isAdMobInitialized = initialized
                android.util.Log.d("Ad", "AdMob初期化完了: $initialized")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // バックグラウンドに移行時に一時停止
        viewModel.onPause()
        binding.visualizerView.stopVisualizer()
    }

    override fun onStop() {
        super.onStop()
        // 完全に見えなくなった時に停止
        viewModel.onStop()

        // 初期設定画面への遷移中はアプリを終了させない
        if (isNavigatingToSetup) {
            isNavigatingToSetup = false
            return
        }

        // AdMobの広告を破棄してからアプリを終了
        // (ClipboardServiceエラー回避のため)
        adView?.destroy()
        // バックグラウンドに移行したらアプリを終了
        // finishAndRemoveTask()を使用してタスクリストからも削除
        finishAndRemoveTask()
        // プロセスを完全に終了させる
        // (AdMobのバックグラウンドスレッドも含めて強制終了)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            android.os.Process.killProcess(android.os.Process.myPid())
            kotlin.system.exitProcess(0)
        }, 500)
    }

    override fun onDestroy() {
        super.onDestroy()
        // アプリ終了時に既読記事情報をクリア
        viewModel.clearReadArticles()
        android.util.Log.d("MainActivity", "onDestroy: 既読記事情報をクリア")
    }

    /**
     * タッチイベントの処理（スマホでのフリック検出）
     */
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // スマホの場合のみGestureDetectorで処理
        if (DeviceUtils.isPhone(applicationContext) && event != null) {
            if (gestureDetector.onTouchEvent(event)) {
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, audioPermission) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun checkAudioPermission() {
        if (!hasAudioPermission()) {
            ActivityCompat.requestPermissions(this, arrayOf(audioPermission), requestCodeAudio)
        }
    }

    /**
     * AdViewを動的に作成（画面幅に合わせたアダプティブバナー）
     * TVデバイスでは設定をスキップ
     * 
     * 注: AdMobのアダプティブバナーを使うためには、AdViewをXMLで定義せずプログラムで作成する必要がある
     * （XMLでadSizeを設定するとsetAdSize()が一度しか呼べないため）
     */
    private fun setupAdView() {
        if (DeviceUtils.isTV(applicationContext)) {
            android.util.Log.d("AdView", "TV device detected, skipping AdView setup")
            return
        }

        // 既に作成済みならスキップ
        if (adView != null) return

        // AdUnitId を文字列リソースから取得
        val adUnitId = getString(R.string.admob_banner_ad_unit_id)

        // 画面幅を取得（displayMetricsを使用）
        val displayMetrics = resources.displayMetrics
        val adWidthDp = (displayMetrics.widthPixels / displayMetrics.density).toInt()
        
        // アダプティブバナーサイズを取得
        val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidthDp)
        
        // AdViewをプログラムで作成
        adView = AdView(this).apply {
            setAdSize(adSize)
            this.adUnitId = adUnitId
        }
        
        // FrameLayoutコンテナに追加
        binding.adViewContainer.addView(adView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ))
        
        android.util.Log.d("AdView", "AdView created dynamically: width=${adWidthDp}dp, adSize=${adSize}, adUnitId=$adUnitId")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodeAudio) {
            if (hasAudioPermission()) {
                android.util.Log.d("Visualizer", "RECORD_AUDIO許可取得。次のtrack更新でVisualizer初期化予定")
                // 現在トラック再生中なら即再初期化試行
                val current = viewModel.uiState.value.currentTrack
                if (current != null) {
                    val sessionId = musicPlayer.getAudioSessionId()
                    if (sessionId != 0 && musicPlayer.isPlaying()) {
                        android.util.Log.d("Visualizer", "permissionResult -> startVisualizer audioSessionId=$sessionId track=${current.title}")
                        binding.visualizerView.startVisualizer(sessionId)
                        lastVisualizerSessionId = sessionId
                    }
                }
            } else {
                android.util.Log.d("Visualizer", "RECORD_AUDIO拒否。フェイク表示継続")
            }
        }
    }

    /**
     * デバッグ情報を更新（DEBUG BUILDのみ）
     */
    private fun updateDebugInfo(state: MainUiState) {
        if (!BuildConfig.DEBUG) {
            binding.debugInfoView.visibility = android.view.View.GONE
            binding.newsDebugView?.visibility = android.view.View.GONE
            return
        }

        binding.debugInfoView.visibility = android.view.View.VISIBLE
        binding.newsDebugView?.visibility = android.view.View.VISIBLE
        
        // TTS ON/OFF状態と次のニュースまでの残り秒数、広告表示状態を表示
        val ttsStatus = if (state.enableTts) "ON" else "OFF"
        val remainingSeconds = state.debugNextNewsRemainingSeconds
        val adStatus = if (state.showAd) "AD表示中 (残り${state.adRemainingSeconds}秒)" else "AD非表示"
        binding.debugInfoView.text = "[DEBUG] TTS: $ttsStatus | 次の記事まで: ${remainingSeconds}秒 | $adStatus"
        
        // NewsDebugViewを更新（RSS取得時刻と記事リスト）
        binding.newsDebugView?.updateDebugInfo(state.debugNewsList, state.debugLastFetchTime)
    }

    /**
     * 広告ビューまたはQR誘導の表示/非表示を制御
     * 
     * スマホ: 広告表示（AdMob）
     * TV: スマホ版QRコード誘導表示
     */
    private fun updateAdView(showAd: Boolean, adRemainingSeconds: Long = 0L) {
        // TVデバイスではQR誘導を表示
        if (DeviceUtils.isTV(applicationContext)) {
            updateQrPromotion(showAd, adRemainingSeconds)
            return
        }
        
        val currentAdView = adView ?: return  // AdViewが未作成なら何もしない
        
        if (showAd && binding.adViewContainer.visibility != android.view.View.VISIBLE) {
            // AdMob初期化済みなら広告を表示
            if (isAdMobInitialized) {
                binding.adViewContainer.visibility = android.view.View.VISIBLE
                binding.adViewContainer.bringToFront() // 最前面に表示
                adRepository.loadAd(currentAdView)
                android.util.Log.d("Ad", "広告表示を開始")
            } else {
                android.util.Log.d("Ad", "AdMob未初期化のため広告スキップ")
            }
        } else if (!showAd && binding.adViewContainer.visibility == android.view.View.VISIBLE) {
            // 広告非表示
            binding.adViewContainer.visibility = android.view.View.GONE
            android.util.Log.d("Ad", "広告を非表示にしました")
        }
        
        // カウントダウン表示の制御
        if (showAd && isAdMobInitialized) {
            binding.adCountdownView.visibility = android.view.View.VISIBLE
            binding.adCountdownView.text = "広告終了まで ${adRemainingSeconds}秒"
            binding.adCountdownView.bringToFront()
        } else {
            binding.adCountdownView.visibility = android.view.View.GONE
        }
    }

    /**
     * TV版スマホQR誘導の表示/非表示を制御
     * 
     * 広告表示タイミングと同じ10秒間、スマホ版ダウンロードQRコードを表示
     */
    private fun updateQrPromotion(show: Boolean, remainingSeconds: Long) {
        if (show) {
            // QRコードが既に生成済みなら設定（事前生成で既に完了しているはず）
            if (qrBitmap != null) {
                binding.qrImageView?.setImageBitmap(qrBitmap)
            }
            
            // QR誘導コンテナを表示
            binding.qrPromotionContainer?.visibility = android.view.View.VISIBLE
            binding.qrPromotionContainer?.bringToFront()
            binding.qrCountdownView?.text = "あと ${remainingSeconds}秒"
        } else {
            // QR誘導非表示
            binding.qrPromotionContainer?.visibility = android.view.View.GONE
        }
    }
}
