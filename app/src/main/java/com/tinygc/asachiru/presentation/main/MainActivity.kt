package com.tinygc.asachiru.presentation.main

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.tinygc.asachiru.presentation.setup.SetupActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import com.tinygc.asachiru.BuildConfig
import com.tinygc.asachiru.databinding.ActivityMainBinding
import com.tinygc.asachiru.domain.util.DeviceUtils
import com.tinygc.asachiru.presentation.common.ViewModelFactory
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

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var viewModelFactory: com.tinygc.asachiru.presentation.common.ViewModelFactory
    // MusicPlayerへのアクセス（Visualizer用 - RepositoryFactoryグローバルシングルトン）
    private val musicPlayer by lazy { viewModelFactory.let { com.tinygc.asachiru.di.RepositoryFactory(applicationContext).getMusicPlayer() } }
    private var lastVisualizerSessionId: Int = -1
    private val audioPermission = android.Manifest.permission.RECORD_AUDIO
    private val requestCodeAudio = 1001
    private var isNavigatingToSetup = false

    // スマホでのフリック検出用GestureDetector
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 画面を常にオンに保つ（アンビエントモード防止）
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // ViewModelFactoryを生成して保持
        viewModelFactory = ViewModelFactory(applicationContext)
        // ViewModelFactoryを使用してViewModelを生成
        viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]

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
                if (abs(diffY) > abs(diffX) * 2 && abs(diffY) > 100) {
                    if (diffY < 0) {
                        // 上フリック: 前のニュースへ
                        viewModel.navigateToPreviousNews()
                    } else {
                        // 下フリック: 次のニュースへ
                        viewModel.navigateToNextNews()
                    }
                    return true
                }
                
                return false
            }

            // シングルタップで詳細表示切り替え（スマホのみ）
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (DeviceUtils.isPhone(applicationContext)) {
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
            binding.keyHintView.visibility = android.view.View.GONE
            binding.settingsButton.setOnClickListener {
                navigateToSetup()
            }
        } else {
            // TV: キー操作ヒントを表示し、設定ボタンは非表示
            binding.settingsButton.visibility = android.view.View.GONE
            binding.keyHintView.visibility = android.view.View.VISIBLE
        }

        observeViewModel()
        checkAudioPermission()
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
                    if (state.newsError != null) {
                        binding.newsView.showError(state.newsError)
                    } else {
                        binding.newsView.updateNews(state.currentNews)
                        binding.newsView.setShowDetail(state.showNewsDetail)
                        binding.newsView.setEnableTts(state.enableTts)
                        binding.newsView.setIsSpeaking(state.isSpeaking)
                        binding.newsView.setProgress(state.newsProgressPercent)
                        
                        // 詳細表示時はNewsViewを全画面表示にする
                        updateNewsViewLayout(state.showNewsDetail)
                    }

                    // 音楽トラック情報の更新
                    binding.musicTrackView.updateMusic(state.currentTrack)

                    // 広告の表示/非表示制御
                    updateAdView(state.showAd)

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
                }
            }
        }
    }
    
    /**
     * NewsViewのレイアウトを詳細表示状態に応じて切り替え
     */
    private fun updateNewsViewLayout(isDetailShown: Boolean) {
        val layoutParams = binding.newsView.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        
        if (isDetailShown) {
            // 詳細表示時: 全画面表示
            layoutParams.width = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT
            layoutParams.height = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT
            layoutParams.setMargins(0, 0, 0, 0)
            
            // 最前面に表示
            binding.newsView.bringToFront()
            // 親Viewの再描画を強制
            (binding.newsView.parent as? android.view.ViewGroup)?.invalidate()
        } else {
            // 通常時: 画面下部に配置
            layoutParams.width = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
            layoutParams.height = resources.displayMetrics.density.toInt() * 100 // 100dp
            layoutParams.setMargins(
                (50 * resources.displayMetrics.density).toInt(),
                0,
                (50 * resources.displayMetrics.density).toInt(),
                (50 * resources.displayMetrics.density).toInt()
            )
            
            // ConstraintLayoutの制約を再設定
            layoutParams.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            layoutParams.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            layoutParams.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
        }
        
        binding.newsView.layoutParams = layoutParams
        binding.newsView.requestLayout()
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
        binding.adView.destroy()
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
            binding.newsDebugView.visibility = android.view.View.GONE
            return
        }

        binding.debugInfoView.visibility = android.view.View.VISIBLE
        binding.newsDebugView.visibility = android.view.View.VISIBLE
        
        // TTS ON/OFF状態と次のニュースまでの残り秒数、広告表示状態を表示
        val ttsStatus = if (state.enableTts) "ON" else "OFF"
        val remainingSeconds = state.debugNextNewsRemainingSeconds
        val adStatus = if (state.showAd) "AD表示中 (残り${state.adRemainingSeconds}秒)" else "AD非表示"
        binding.debugInfoView.text = "[DEBUG] TTS: $ttsStatus | 次の記事まで: ${remainingSeconds}秒 | $adStatus"
        
        // NewsDebugViewを更新（RSS取得時刻と記事リスト）
        binding.newsDebugView.updateDebugInfo(state.debugNewsList, state.debugLastFetchTime)
    }

    /**
     * 広告ビューの表示/非表示を制御
     */
    private fun updateAdView(showAd: Boolean) {
        if (showAd && binding.adView.visibility != android.view.View.VISIBLE) {
            // AdMob初期化と広告読み込み
            lifecycleScope.launch {
                val adRepository = com.tinygc.asachiru.data.repository.AdRepository(applicationContext)
                val initialized = adRepository.initializeMobileAds()
                if (initialized) {
                    binding.adView.visibility = android.view.View.VISIBLE
                    binding.adView.bringToFront() // 最前面に表示
                    adRepository.loadAd(binding.adView)
                    android.util.Log.d("Ad", "広告表示を開始")
                }
            }
        } else if (!showAd && binding.adView.visibility == android.view.View.VISIBLE) {
            // 広告非表示
            binding.adView.visibility = android.view.View.GONE
            android.util.Log.d("Ad", "広告を非表示にしました")
        }
    }
}
