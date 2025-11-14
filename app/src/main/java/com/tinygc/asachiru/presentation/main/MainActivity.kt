package com.tinygc.asachiru.presentation.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ViewModelFactoryを使用してViewModelを生成
        viewModel = ViewModelProvider(this, ViewModelFactory(applicationContext))[MainViewModel::class.java]

        observeViewModel()
    }

    /**
     * ViewModelの状態を監視してUIを更新
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
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
                    }

                    // ビジュアライザーは音楽再生時に自動的に動作
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        // Android TVアプリとして、バックグラウンドに移行したら終了する
        // ホームボタンや他のアプリへの切り替え時に呼ばれる
        finish()
    }
}
