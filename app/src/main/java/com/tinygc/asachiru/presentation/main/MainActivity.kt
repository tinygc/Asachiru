package com.tinygc.asachiru.presentation.main

import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import com.tinygc.asachiru.BuildConfig
import com.tinygc.asachiru.databinding.ActivityMainBinding
import com.tinygc.asachiru.presentation.common.ViewModelFactory
import kotlinx.coroutines.launch

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ViewModelFactoryを生成して保持
        viewModelFactory = ViewModelFactory(applicationContext)
        // ViewModelFactoryを使用してViewModelを生成
        viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]

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
                        
                        // 詳細表示時はNewsViewを全画面表示にする
                        updateNewsViewLayout(state.showNewsDetail)
                    }

                    // 音楽トラック情報の更新
                    binding.musicTrackView.updateMusic(state.currentTrack)

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
                // 決定キー: 詳細表示切り替え
                viewModel.toggleNewsDetail()
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
        
        // TTS ON/OFF状態のみ表示（RSS情報はNewsDebugViewに集約）
        val ttsStatus = if (state.enableTts) "ON" else "OFF"
        binding.debugInfoView.text = "[DEBUG] TTS: $ttsStatus"
        
        // NewsDebugViewを更新（RSS取得時刻と記事リスト）
        binding.newsDebugView.updateDebugInfo(state.debugNewsList, state.debugLastFetchTime)
    }
}
