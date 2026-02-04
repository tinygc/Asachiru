package com.tinygc.asachiru.presentation.setup

import android.app.AlertDialog
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
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
import androidx.recyclerview.widget.LinearLayoutManager
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
    private lateinit var rssPresetAdapter: RssPresetAdapter

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

        // RSSプリセット一覧（チェックボックスリスト）
        val presets = RssPresets.PRESETS.toList()
        rssPresetAdapter = RssPresetAdapter(
            presets = presets,
            selectedFeeds = emptyList(),
            onItemChecked = { feed, isChecked ->
                if (isChecked) {
                    viewModel.toggleRssFeed(feed)
                } else {
                    viewModel.toggleRssFeed(feed)
                }
            }
        )
        binding.rssPresetsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SetupActivity)
            adapter = rssPresetAdapter
        }

        // カスタムURL追加ボタン
        binding.addCustomRssButton.setOnClickListener {
            showAddCustomRssDialog()
        }

        // カスタムRSSリスト（初期は空）
        val customRssAdapter = CustomRssAdapter(
            customFeeds = emptyList(),
            onDelete = { feedId ->
                viewModel.removeCustomRss(feedId)
            }
        )
        binding.customRssRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SetupActivity)
            adapter = customRssAdapter
        }

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
            val currentState = viewModel.uiState.value
            val hasValidRss = currentState.isRssFeedsValid || currentState.isRssUrlValid
            
            if (!currentState.isPostalCodeValid) {
                Toast.makeText(this, "郵便番号を正しく入力してください（7桁の数字）", Toast.LENGTH_LONG).show()
                binding.postalCodeEditText.requestFocus()
            } else if (!hasValidRss) {
                Toast.makeText(this, "RSSフィードを1つ以上選択してください", Toast.LENGTH_LONG).show()
                binding.rssPresetsRecyclerView.requestFocus()
            } else {
                viewModel.saveSettings()
            }
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

        // RSSプリセットチェックボックスリスト更新
        rssPresetAdapter.updateSelectedFeeds(state.selectedRssFeeds)

        // カスタムRSSリスト更新
        if (state.customRssFeeds.isNotEmpty()) {
            binding.customRssRecyclerView.visibility = View.VISIBLE
            val customRssAdapter = CustomRssAdapter(
                customFeeds = state.customRssFeeds,
                onDelete = { feedId ->
                    viewModel.removeCustomRss(feedId)
                    // 削除後、すぐに保存（バリデーション無視・ナビゲーションなし）
                    viewModel.saveSettings(force = true, navigateOnSuccess = false)
                }
            )
            binding.customRssRecyclerView.adapter = customRssAdapter
        } else {
            binding.customRssRecyclerView.visibility = View.GONE
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

        // 保存ボタンの有効/無効（保存中のみ無効化、バリデーションはクリック時に行う）
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

    /**
     * ソフトキーボードを表示
     *
     * @param view フォーカスされたView
     */
    private fun showKeyboard(view: View) {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        inputMethodManager?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * カスタムURL追加ダイアログを表示
     */
    private fun showAddCustomRssDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_custom_rss, null)
        val urlEditText = dialogView.findViewById<EditText>(R.id.custom_rss_url_input)
        val nameEditText = dialogView.findViewById<EditText>(R.id.custom_rss_name_input)

        AlertDialog.Builder(this)
            .setTitle("カスタムURL追加")
            .setView(dialogView)
            .setPositiveButton("追加") { _, _ ->
                val url = urlEditText.text.toString().trim()
                val name = nameEditText.text.toString().trim()
                if (url.isNotEmpty()) {
                    viewModel.addCustomRss(url, name.ifEmpty { null })
                    // 追加後、すぐに保存（バリデーション無視・ナビゲーションなし）
                    viewModel.saveSettings(force = true, navigateOnSuccess = false)
                } else {
                    Toast.makeText(this, "URLを入力してください", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }
}
