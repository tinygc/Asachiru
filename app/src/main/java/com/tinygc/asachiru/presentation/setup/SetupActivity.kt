package com.tinygc.asachiru.presentation.setup

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.tinygc.asachiru.data.RssPresets
import com.tinygc.asachiru.databinding.ActivitySetupBinding
import com.tinygc.asachiru.presentation.common.ViewModelFactory
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

        // ViewModelFactoryを使用してViewModelを生成
        viewModel = ViewModelProvider(this, ViewModelFactory(applicationContext))[SetupViewModel::class.java]

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
                viewModel.updatePostalCode(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // ニュース読み上げ間隔
        binding.newsIntervalSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // ドラッグ中は表示だけ更新（ViewModelは更新しない）
                binding.newsIntervalTextView.text = "$progress 分"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // ドラッグ終了時にViewModelを更新
                seekBar?.let {
                    viewModel.updateNewsInterval(it.progress)
                }
            }
        })

        // RSSプリセットSpinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, RssPresets.PRESET_NAMES)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.rssPresetSpinner.adapter = adapter

        binding.rssPresetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = RssPresets.PRESET_NAMES[position]
                
                // "カスタムURL入力"選択時はEditTextを表示
                if (selected == RssPresets.CUSTOM_URL) {
                    binding.rssCustomUrlEditText.visibility = View.VISIBLE
                    viewModel.updateRssPreset(selected)
                } else if (selected != "選択してください") {
                    binding.rssCustomUrlEditText.visibility = View.GONE
                    val url = RssPresets.getUrl(selected)
                    viewModel.updateRssUrl(url ?: "")
                    viewModel.updateRssPreset(selected)
                } else {
                    binding.rssCustomUrlEditText.visibility = View.GONE
                    viewModel.updateRssUrl("")
                    viewModel.updateRssPreset(null)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // 何もしない
            }
        }

        // カスタムURL入力
        binding.rssCustomUrlEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (binding.rssCustomUrlEditText.visibility == View.VISIBLE) {
                    viewModel.updateRssUrl(s?.toString() ?: "")
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // TTS有効化チェックボックス
        binding.enableTtsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateEnableTts(isChecked)
        }

        // 保存ボタン
        binding.saveButton.setOnClickListener {
            viewModel.saveSettings()
        }
    }

    /**
     * ViewModelの状態を監視してUIを更新
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    /**
     * UI更新
     *
     * @param state UI状態
     */
    private fun updateUI(state: SetupUiState) {
        // バリデーションエラー表示
        if (!state.isPostalCodeValid && state.postalCode.isNotEmpty()) {
            binding.postalCodeEditText.error = "郵便番号は7桁の数字で入力してください"
        } else {
            binding.postalCodeEditText.error = null
        }

        // 保存中はボタンを無効化
        binding.saveButton.isEnabled = !state.isSaving

        // エラーメッセージ表示
        state.saveError?.let {
            Toast.makeText(this, "保存に失敗しました: $it", Toast.LENGTH_LONG).show()
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
