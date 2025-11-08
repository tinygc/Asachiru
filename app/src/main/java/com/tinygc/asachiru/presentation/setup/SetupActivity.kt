package com.tinygc.asachiru.presentation.setup

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.tinygc.asachiru.databinding.ActivitySetupBinding
import com.tinygc.asachiru.presentation.main.MainActivity
import kotlinx.coroutines.launch

/**
 * 初回設定画面のActivity
 *
 * 郵便番号とニュース読み上げ間隔を設定します。
 * 設定完了後はMainActivityに遷移します。
 *
 * TODO: ViewModelFactoryを実装して、依存性注入を行う
 */
class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private lateinit var viewModel: SetupViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TODO: ViewModelFactoryを使用してViewModelを生成
        // viewModel = ViewModelProvider(this, ViewModelFactory())[SetupViewModel::class.java]

        setupViews()
        observeViewModel()
    }

    /**
     * ビューの初期設定
     */
    private fun setupViews() {
        // 郵便番号入力
        binding.postalCodeEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // TODO: ViewModelの実装後に有効化
                // viewModel.updatePostalCode(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // ニュース読み上げ間隔
        binding.newsIntervalSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.newsIntervalTextView.text = "$progress 分"
                // TODO: ViewModelの実装後に有効化
                // viewModel.updateNewsInterval(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 保存ボタン
        binding.saveButton.setOnClickListener {
            // TODO: ViewModelの実装後に有効化
            // viewModel.saveSettings()
        }
    }

    /**
     * ViewModelの状態を監視してUIを更新
     */
    private fun observeViewModel() {
        // TODO: ViewModelの実装後に有効化
        /*
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
        */
    }

    /**
     * UI更新
     *
     * @param state UI状態
     */
    private fun updateUI(state: SetupUiState) {
        // バリデーションエラー表示
        if (!state.isPostalCodeValid) {
            binding.postalCodeEditText.error = "郵便番号は7桁の数字で入力してください"
        }

        // 保存中はボタンを無効化
        binding.saveButton.isEnabled = !state.isSaving

        // エラーメッセージ表示
        state.saveError?.let {
            // TODO: Toastなどでエラー表示
        }

        // 保存完了したらメイン画面へ遷移
        if (state.isComplete) {
            navigateToMain()
        }
    }

    /**
     * メイン画面へ遷移
     */
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
