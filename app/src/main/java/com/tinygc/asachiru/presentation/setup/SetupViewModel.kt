package com.tinygc.asachiru.presentation.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.Settings
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
    private val saveSettingsUseCase: SaveSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

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

    /**
     * ニュース読み上げ間隔を更新
     *
     * @param interval ニュース読み上げ間隔（1～60分）
     */
    fun updateNewsInterval(interval: Int) {
        _uiState.update {
            it.copy(
                newsInterval = interval,
                isNewsIntervalValid = validateNewsInterval(interval)
            )
        }
    }

    /**
     * RSS URLを更新
     */
    fun updateRssUrl(url: String) {
        _uiState.update { it.copy(rssUrl = url) }
    }

    /**
     * TTS有効化を更新
     */
    fun updateEnableTts(enable: Boolean) {
        _uiState.update { it.copy(enableTts = enable) }
    }

    /**
     * RSSプリセット名を更新
     */
    fun updateRssPreset(preset: String?) {
        _uiState.update { it.copy(rssPreset = preset) }
    }

    /**
     * 設定を保存
     *
     * バリデーションが全て通過している場合のみ保存を実行します。
     */
    fun saveSettings() {
        val currentState = _uiState.value

        // バリデーションチェック
        if (!currentState.isPostalCodeValid || !currentState.isNewsIntervalValid) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val settings = Settings(
                postalCode = currentState.postalCode,
                newsIntervalMinutes = currentState.newsInterval,
                rssUrl = currentState.rssUrl,
                enableTts = currentState.enableTts,
                rssPreset = currentState.rssPreset
            )

            when (val result = saveSettingsUseCase(settings)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveError = null,
                            isComplete = true
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
}
