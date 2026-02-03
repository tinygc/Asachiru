package com.tinygc.asachiru.presentation.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.RssFeed
import com.tinygc.asachiru.domain.entity.Settings
import com.tinygc.asachiru.domain.usecase.settings.GetSettingsUseCase
import com.tinygc.asachiru.domain.usecase.settings.SaveSettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 設定画面のViewModel
 *
 * ユーザー入力のバリデーションと設定の保存を管理します。
 */
class SetupViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val saveSettingsUseCase: SaveSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    init {
        loadExistingSettings()
    }

    /**
     * 既存の設定を読み込む
     */
    private fun loadExistingSettings() {
        viewModelScope.launch {
            try {
                val settings = getSettingsUseCase()
                // 新形式・旧形式を統合した実際の有効フィードを取得
                val activeFeeds = settings.getActiveRssFeeds()
                val customFeeds = activeFeeds.filter { it.isCustom }

                // 設定が存在する場合はUIStateに反映
                if (settings.postalCode.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            postalCode = settings.postalCode,
                            isPostalCodeValid = validatePostalCode(settings.postalCode),
                            rssUrl = settings.rssUrl ?: "",
                            isRssUrlValid = validateRssUrl(settings.rssUrl ?: ""),
                            selectedRssFeeds = activeFeeds,
                            customRssFeeds = customFeeds,
                            isRssFeedsValid = activeFeeds.isNotEmpty(),
                            enableTts = settings.enableTts,
                            enableBgm = settings.enableBgm,
                            rssPreset = settings.rssPreset,
                            newsInterval = settings.newsIntervalMinutes
                        )
                    }
                }
            } catch (e: Exception) {
                // 設定読み込みエラーは無視（初回起動時など）
            }
        }
    }

    /**
     * 郵便番号を更新
     *
     * @param postalCode 郵便番号（7桁、ハイフンなし）
     */
    fun updatePostalCode(postalCode: String) {
        _uiState.update {
            it.copy(
                postalCode = postalCode,
                isPostalCodeValid = validatePostalCode(postalCode)
            )
        }
    }

    // ニュース読み上げ間隔は画面から削除したため、
    // ViewModel側ではデフォルト値を保持するのみとし、明示的な更新は行わない

    /**
     * RSS URLを更新（旧形式、後方互換性のため残す）
     */
    fun updateRssUrl(url: String) {
        _uiState.update { 
            it.copy(
                rssUrl = url,
                isRssUrlValid = validateRssUrl(url)
            ) 
        }
    }

    /**
     * RSSフィード選択切り替え
     * @param feed 選択/解除するRSSフィード
     */
    fun toggleRssFeed(feed: RssFeed) {
        _uiState.update { state ->
            val currentFeeds = state.selectedRssFeeds
            val updatedFeeds = if (currentFeeds.any { it.id == feed.id }) {
                // 既に選択されていたら解除
                currentFeeds.filter { it.id != feed.id }
            } else {
                // 未選択なら追加
                currentFeeds + feed
            }
            state.copy(
                selectedRssFeeds = updatedFeeds,
                isRssFeedsValid = updatedFeeds.isNotEmpty()
            )
        }
    }

    /**
     * カスタムRSS追加
     * @param url RSS URL
     * @param name カスタム名（任意）
     */
    fun addCustomRss(url: String, name: String? = null) {
        val customFeed = RssFeed.fromCustomUrl(url, name)
        _uiState.update { state ->
            state.copy(
                customRssFeeds = state.customRssFeeds + customFeed,
                selectedRssFeeds = state.selectedRssFeeds + customFeed,
                isRssFeedsValid = true
            )
        }
    }

    /**
     * カスタムRSS削除
     * @param feedId 削除するRSSフィードのID
     */
    fun removeCustomRss(feedId: String) {
        _uiState.update { state ->
            val updatedCustom = state.customRssFeeds.filter { it.id != feedId }
            val updatedSelected = state.selectedRssFeeds.filter { it.id != feedId }
            state.copy(
                customRssFeeds = updatedCustom,
                selectedRssFeeds = updatedSelected,
                isRssFeedsValid = updatedSelected.isNotEmpty()
            )
        }
    }

    /**
     * TTS有効化を更新
     */
    fun updateEnableTts(enable: Boolean) {
        _uiState.update { it.copy(enableTts = enable) }
    }

    /**
     * BGM有効化を更新
     */
    fun updateEnableBgm(enable: Boolean) {
        _uiState.update { it.copy(enableBgm = enable) }
    }

    /**
     * RSSプリセット名を更新（旧形式、後方互換性のため残す）
     */
    fun updateRssPreset(preset: String?) {
        _uiState.update { it.copy(rssPreset = preset) }
    }

    /**
     * 設定を保存
     *
     * バリデーションが全て通過している場合のみ保存を実行します。
     */
    fun saveSettings(force: Boolean = false, navigateOnSuccess: Boolean = true) {
        val currentState = _uiState.value

        // バリデーションチェック
        // 新形式（selectedRssFeeds）か旧形式（rssUrl）のどちらかが有効ならOK
        val hasValidRss = currentState.isRssFeedsValid || currentState.isRssUrlValid
        if (!force && (!currentState.isPostalCodeValid || !hasValidRss)) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val settings = Settings(
                postalCode = currentState.postalCode,
                newsIntervalMinutes = currentState.newsInterval,
                rssUrl = currentState.rssUrl,
                enableTts = currentState.enableTts,
                enableBgm = currentState.enableBgm,
                rssPreset = currentState.rssPreset,
                rssFeeds = currentState.selectedRssFeeds
            )

            when (val result = saveSettingsUseCase(settings, force)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveError = null,
                            isComplete = navigateOnSuccess
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveError = result.exception.message
                        )
                    }
                }
            }
        }
    }

    /**
     * 郵便番号のバリデーション
     *
     * @param postalCode 郵便番号
     * @return 7桁の数字のみの場合true
     */
    private fun validatePostalCode(postalCode: String): Boolean {
        return postalCode.length == 7 && postalCode.all { it.isDigit() }
    }

    /**
     * ニュース読み上げ間隔のバリデーション
     *
     * @param interval ニュース読み上げ間隔
     * @return 1～60の範囲内の場合true
     */
    private fun validateNewsInterval(interval: Int): Boolean {
        return interval in 1..60
    }

    /**
     * RSS URLのバリデーション
     *
     * @param url RSS URL
     * @return 空でない場合true
     */
    private fun validateRssUrl(url: String): Boolean {
        return url.isNotBlank()
    }
}
