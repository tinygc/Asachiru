package com.tinygc.asachiru.presentation.setup

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.tinygc.asachiru.R
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.tinygc.asachiru.data.RssPresets
import com.tinygc.asachiru.databinding.ActivitySetupBinding
import com.tinygc.asachiru.domain.util.DeviceUtils
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
        if (!isTelevision()) {
            enableEdgeToEdge()
        }

        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!isTelevision()) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                // 上下のinsetsのみ適用
                v.setPadding(0, bars.top, 0, bars.bottom)
                WindowInsetsCompat.CONSUMED
            }
        }

        // ViewModelFactoryを使用してViewModelを生成
        viewModel = ViewModelProvider(this, ViewModelFactory(applicationContext))[SetupViewModel::class.java]

        setupViews()
        observeViewModel()
    }

    private fun isTelevision(): Boolean {
        val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
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
                // 入力中はエラー表示をクリア
                binding.postalCodeEditText.error = null
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // 郵便番号入力欄のフォーカス変更時にバリデーション＆キーボード表示
        binding.postalCodeEditText.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                // スマホの場合、フォーカス取得時にキーボードを表示
                if (DeviceUtils.isPhone(applicationContext)) {
                    showKeyboard(view)
                }
            } else {
                val currentState = viewModel.uiState.value
                if (!currentState.isPostalCodeValid && currentState.postalCode.isNotEmpty()) {
                    binding.postalCodeEditText.error = "郵便番号は7桁の数字で入力してください"
                }
            }
        }

        // ニュース間隔設定は一旦UIから削除

        // RSSプリセットSpinner（カスタムレイアウトで白文字表示）
        val adapter = ArrayAdapter(this, R.layout.item_rss_preset_spinner, RssPresets.PRESET_NAMES)
        adapter.setDropDownViewResource(R.layout.item_rss_preset_dropdown)
        binding.rssPresetSpinner.adapter = adapter

        binding.rssPresetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = RssPresets.PRESET_NAMES[position]
                
                // "カスタムURL入力"選択時はEditTextを表示してフォーカス
                if (selected == RssPresets.CUSTOM_URL) {
                    binding.rssCustomUrlEditText.visibility = View.VISIBLE
                    viewModel.updateRssPreset(selected)
                    // フォーカス移動は少し遅延させる
                    binding.rssCustomUrlEditText.postDelayed({
                        binding.rssCustomUrlEditText.requestFocus()
                        // スマホの場合、キーボードを表示
                        if (DeviceUtils.isPhone(applicationContext)) {
                            showKeyboard(binding.rssCustomUrlEditText)
                        }
                    }, 100)
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

        // BGM有効化チェックボックス
        binding.enableBgmCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateEnableBgm(isChecked)
        }

        // 保存ボタン
        // スマホではfocusableInTouchModeを無効化（1タップで即反応させる）
        if (DeviceUtils.isPhone(applicationContext)) {
            binding.saveButton.isFocusableInTouchMode = false
        }
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
        // 郵便番号の値を反映（TextWatcherの無限ループを避けるため、異なる場合のみ更新）
        if (binding.postalCodeEditText.text.toString() != state.postalCode) {
            binding.postalCodeEditText.setText(state.postalCode)
            binding.postalCodeEditText.setSelection(state.postalCode.length)
        }

        // RSSプリセットの選択を反映
        state.rssPreset?.let { preset ->
            val position = RssPresets.PRESET_NAMES.indexOf(preset)
            if (position >= 0 && binding.rssPresetSpinner.selectedItemPosition != position) {
                binding.rssPresetSpinner.setSelection(position)
            }
        }

        // カスタムURLの値を反映（カスタムURL選択時のみ）
        if (state.rssPreset == RssPresets.CUSTOM_URL) {
            binding.rssCustomUrlEditText.visibility = View.VISIBLE
            if (binding.rssCustomUrlEditText.text.toString() != state.rssUrl) {
                binding.rssCustomUrlEditText.setText(state.rssUrl)
            }
        }

        // チェックボックスの状態を反映
        if (binding.enableTtsCheckbox.isChecked != state.enableTts) {
            binding.enableTtsCheckbox.isChecked = state.enableTts
        }
        if (binding.enableBgmCheckbox.isChecked != state.enableBgm) {
            binding.enableBgmCheckbox.isChecked = state.enableBgm
        }

        // 郵便番号バリデーションエラーは、フォーカス変更時にのみ表示するため、
        // ここでは何もしない（入力中に表示されないようにするため）

        // RSS URLのバリデーションエラー表示
        if (!state.isRssUrlValid && state.rssUrl.isEmpty() && binding.rssCustomUrlEditText.visibility == View.VISIBLE) {
            binding.rssCustomUrlEditText.error = "URLを入力してください"
        } else {
            binding.rssCustomUrlEditText.error = null
        }

        // 保存ボタンの有効/無効（ニュース間隔はデフォルト値固定のためチェック対象外）
        binding.saveButton.isEnabled = !state.isSaving &&
            state.isPostalCodeValid &&
            state.isRssUrlValid

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

    /**
     * ソフトキーボードを表示
     *
     * @param view フォーカスされたView
     */
    private fun showKeyboard(view: View) {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        inputMethodManager?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
}
