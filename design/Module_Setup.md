# モジュール設計書 - 初回設定機能

## 1. 概要

初回設定機能は、アプリ初回起動時にユーザーに郵便番号とニュース読み上げ間隔を設定してもらう機能。

---

## 2. 機能要件（再掲）

### 2.1 設定項目
1. 郵便番号（7桁、ハイフンなし）
2. ニュース読み上げ間隔（1～60分）

### 2.2 設定タイミング
- アプリ初回起動時に設定画面を表示

### 2.3 設定の保存
- 入力された設定値を永続化
- アプリ再起動後も設定を保持

### 2.4 バリデーション
- 郵便番号: 7桁の数字のみ許可
- ニュース読み上げ間隔: 1～60の整数のみ許可

---

## 3. アーキテクチャ設計

### 3.1 レイヤー構成

```
[Presentation Layer]
  - SetupActivity
  - SetupViewModel

[Domain Layer]
  - SaveSettingsUseCase
  - GetSettingsUseCase
  - CheckSettingsExistUseCase
  - Settings (Entity)
  - SettingsRepository (Interface)

[Data Layer]
  - SettingsRepositoryImpl
  - SettingsLocalDataSource
```

---

## 4. クラス設計

### 4.1 Domain Layer

#### 4.1.1 Settings Entity

```kotlin
package com.tinygc.asachiru.domain.entity

/**
 * 設定を表すエンティティ
 */
data class Settings(
    val postalCode: String,
    val newsIntervalMinutes: Int
) {
    /**
     * 設定が有効かチェック
     */
    fun isValid(): Boolean {
        return isPostalCodeValid() && isNewsIntervalValid()
    }

    /**
     * 郵便番号が有効かチェック
     */
    fun isPostalCodeValid(): Boolean {
        return postalCode.length == 7 && postalCode.all { it.isDigit() }
    }

    /**
     * ニュース読み上げ間隔が有効かチェック
     */
    fun isNewsIntervalValid(): Boolean {
        return newsIntervalMinutes in 1..60
    }

    companion object {
        /**
         * デフォルト設定
         */
        val DEFAULT = Settings(
            postalCode = "",
            newsIntervalMinutes = 30
        )
    }
}
```

#### 4.1.2 SettingsRepository Interface

```kotlin
package com.tinygc.asachiru.domain.repository

import com.tinygc.asachiru.domain.entity.Settings
import com.tinygc.asachiru.domain.common.Result

/**
 * 設定を管理するリポジトリのインターフェース
 */
interface SettingsRepository {
    /**
     * 設定を取得
     * @return 設定
     */
    suspend fun getSettings(): Settings

    /**
     * 設定を保存
     * @param settings 設定
     * @return 保存結果（Result型）
     */
    suspend fun saveSettings(settings: Settings): Result<Unit>

    /**
     * 設定が存在するかチェック
     * @return 設定が存在する場合true
     */
    suspend fun hasSettings(): Boolean
}
```

#### 4.1.3 SaveSettingsUseCase

```kotlin
package com.tinygc.asachiru.domain.usecase.settings

import com.tinygc.asachiru.domain.entity.Settings
import com.tinygc.asachiru.domain.repository.SettingsRepository
import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.common.AppException

/**
 * 設定を保存するユースケース
 */
class SaveSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    /**
     * 設定を保存
     * @param settings 設定
     * @return 保存結果（Result型）
     */
    suspend operator fun invoke(settings: Settings): Result<Unit> {
        return if (settings.isValid()) {
            settingsRepository.saveSettings(settings)
        } else {
            Result.Error(AppException.SettingsException("Invalid settings"))
        }
    }
}
```

#### 4.1.4 GetSettingsUseCase

```kotlin
package com.tinygc.asachiru.domain.usecase.settings

import com.tinygc.asachiru.domain.entity.Settings
import com.tinygc.asachiru.domain.repository.SettingsRepository

/**
 * 設定を取得するユースケース
 */
class GetSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    /**
     * 設定を取得
     * @return 設定
     */
    suspend operator fun invoke(): Settings {
        return settingsRepository.getSettings()
    }
}
```

#### 4.1.5 CheckSettingsExistUseCase

```kotlin
package com.tinygc.asachiru.domain.usecase.settings

import com.tinygc.asachiru.domain.repository.SettingsRepository

/**
 * 設定が存在するかチェックするユースケース
 */
class CheckSettingsExistUseCase(
    private val settingsRepository: SettingsRepository
) {
    /**
     * 設定が存在するかチェック
     * @return 設定が存在する場合true
     */
    suspend operator fun invoke(): Boolean {
        return settingsRepository.hasSettings()
    }
}
```

### 4.2 Data Layer

#### 4.2.1 SettingsLocalDataSource

```kotlin
package com.tinygc.asachiru.data.datasource.local

import android.content.SharedPreferences
import com.tinygc.asachiru.domain.entity.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 設定をローカルに保存・取得するデータソース
 */
class SettingsLocalDataSource(
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val KEY_POSTAL_CODE = "postal_code"
        private const val KEY_NEWS_INTERVAL = "news_interval"
    }

    /**
     * 設定を取得
     * @return 設定
     */
    suspend fun loadSettings(): Settings = withContext(Dispatchers.IO) {
        val postalCode = sharedPreferences.getString(KEY_POSTAL_CODE, "") ?: ""
        val newsInterval = sharedPreferences.getInt(KEY_NEWS_INTERVAL, 30)

        Settings(
            postalCode = postalCode,
            newsIntervalMinutes = newsInterval
        )
    }

    /**
     * 設定を保存
     * @param settings 設定
     */
    suspend fun saveSettings(settings: Settings) = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .putString(KEY_POSTAL_CODE, settings.postalCode)
            .putInt(KEY_NEWS_INTERVAL, settings.newsIntervalMinutes)
            .apply()
    }

    /**
     * 設定が存在するかチェック
     * @return 設定が存在する場合true
     */
    suspend fun hasSettings(): Boolean = withContext(Dispatchers.IO) {
        sharedPreferences.contains(KEY_POSTAL_CODE)
    }
}
```

#### 4.2.2 SettingsRepositoryImpl

```kotlin
package com.tinygc.asachiru.data.repository

import com.tinygc.asachiru.data.datasource.local.SettingsLocalDataSource
import com.tinygc.asachiru.domain.entity.Settings
import com.tinygc.asachiru.domain.repository.SettingsRepository
import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.common.AppException

/**
 * SettingsRepositoryの実装
 */
class SettingsRepositoryImpl(
    private val settingsLocalDataSource: SettingsLocalDataSource
) : SettingsRepository {

    override suspend fun getSettings(): Settings {
        return settingsLocalDataSource.loadSettings()
    }

    override suspend fun saveSettings(settings: Settings): Result<Unit> {
        return try {
            settingsLocalDataSource.saveSettings(settings)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppException.SettingsException(e.message ?: "Failed to save settings"))
        }
    }

    override suspend fun hasSettings(): Boolean {
        return settingsLocalDataSource.hasSettings()
    }
}
```

### 4.3 Presentation Layer

#### 4.3.1 SetupUiState

```kotlin
package com.tinygc.asachiru.presentation.setup

/**
 * 設定画面のUI状態
 */
data class SetupUiState(
    val postalCode: String = "",
    val newsInterval: Int = 30,
    val isPostalCodeValid: Boolean = true,
    val isNewsIntervalValid: Boolean = true,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val isComplete: Boolean = false
)
```

#### 4.3.2 SetupViewModel

```kotlin
package com.tinygc.asachiru.presentation.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinygc.asachiru.domain.entity.Settings
import com.tinygc.asachiru.domain.usecase.settings.SaveSettingsUseCase
import com.tinygc.asachiru.domain.common.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 設定画面のViewModel
 */
class SetupViewModel(
    private val saveSettingsUseCase: SaveSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    /**
     * 郵便番号を更新
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
     * 設定を保存
     */
    fun saveSettings() {
        val currentState = _uiState.value

        if (!currentState.isPostalCodeValid || !currentState.isNewsIntervalValid) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val settings = Settings(
                postalCode = currentState.postalCode,
                newsIntervalMinutes = currentState.newsInterval
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
     */
    private fun validatePostalCode(postalCode: String): Boolean {
        return postalCode.length == 7 && postalCode.all { it.isDigit() }
    }

    /**
     * ニュース読み上げ間隔のバリデーション
     */
    private fun validateNewsInterval(interval: Int): Boolean {
        return interval in 1..60
    }
}
```

#### 4.3.3 SetupActivity

```kotlin
package com.tinygc.asachiru.presentation.setup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.tinygc.asachiru.R
import com.tinygc.asachiru.presentation.common.ViewModelFactory
import com.tinygc.asachiru.presentation.main.MainActivity
import kotlinx.coroutines.launch

/**
 * 初回設定画面のActivity
 */
class SetupActivity : AppCompatActivity() {

    private lateinit var viewModel: SetupViewModel
    private lateinit var postalCodeEditText: EditText
    private lateinit var newsIntervalSeekBar: SeekBar
    private lateinit var newsIntervalTextView: TextView
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        viewModel = ViewModelProvider(this, ViewModelFactory())
            .get(SetupViewModel::class.java)

        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        postalCodeEditText = findViewById(R.id.postal_code_edit_text)
        newsIntervalSeekBar = findViewById(R.id.news_interval_seek_bar)
        newsIntervalTextView = findViewById(R.id.news_interval_text_view)
        saveButton = findViewById(R.id.save_button)

        // 郵便番号入力
        postalCodeEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                viewModel.updatePostalCode(postalCodeEditText.text.toString())
            }
        }

        // ニュース読み上げ間隔
        newsIntervalSeekBar.min = 1
        newsIntervalSeekBar.max = 60
        newsIntervalSeekBar.progress = 30
        newsIntervalSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                newsIntervalTextView.text = "$progress 分"
                viewModel.updateNewsInterval(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 保存ボタン
        saveButton.setOnClickListener {
            viewModel.saveSettings()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateUI(state)
            }
        }
    }

    private fun updateUI(state: SetupUiState) {
        // バリデーションエラー表示
        if (!state.isPostalCodeValid) {
            postalCodeEditText.error = "郵便番号は7桁の数字で入力してください"
        }

        // 保存中はボタンを無効化
        saveButton.isEnabled = !state.isSaving

        // エラーメッセージ表示
        state.saveError?.let {
            // Toastなどでエラー表示
        }

        // 保存完了したらメイン画面へ遷移
        if (state.isComplete) {
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
```

#### 4.3.4 SplashActivity（初回設定チェック用）

```kotlin
package com.tinygc.asachiru.presentation.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.tinygc.asachiru.presentation.common.ViewModelFactory
import com.tinygc.asachiru.presentation.main.MainActivity
import com.tinygc.asachiru.presentation.setup.SetupActivity
import kotlinx.coroutines.launch

/**
 * スプラッシュ画面（初回設定チェック）
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var viewModel: SplashViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this, ViewModelFactory())
            .get(SplashViewModel::class.java)

        checkSettings()
    }

    private fun checkSettings() {
        lifecycleScope.launch {
            val hasSettings = viewModel.checkSettings()

            if (hasSettings) {
                navigateToMain()
            } else {
                navigateToSetup()
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToSetup() {
        val intent = Intent(this, SetupActivity::class.java)
        startActivity(intent)
        finish()
    }
}
```

#### 4.3.5 SplashViewModel

```kotlin
package com.tinygc.asachiru.presentation.splash

import androidx.lifecycle.ViewModel
import com.tinygc.asachiru.domain.usecase.settings.CheckSettingsExistUseCase

/**
 * スプラッシュ画面のViewModel
 */
class SplashViewModel(
    private val checkSettingsExistUseCase: CheckSettingsExistUseCase
) : ViewModel() {

    /**
     * 設定が存在するかチェック
     * @return 設定が存在する場合true
     */
    suspend fun checkSettings(): Boolean {
        return checkSettingsExistUseCase()
    }
}
```

---

## 5. テスト設計

### 5.1 単体テスト

#### 5.1.1 SaveSettingsUseCaseTest

```kotlin
package com.tinygc.asachiru.domain.usecase.settings

import com.tinygc.asachiru.domain.entity.Settings
import com.tinygc.asachiru.domain.repository.SettingsRepository
import com.tinygc.asachiru.domain.common.Result
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SaveSettingsUseCaseTest {

    @Mock
    private lateinit var settingsRepository: SettingsRepository

    private lateinit var useCase: SaveSettingsUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        useCase = SaveSettingsUseCase(settingsRepository)
    }

    @Test
    fun `invoke should save settings when valid`() = runBlocking {
        // Arrange
        val settings = Settings("1000001", 30)
        whenever(settingsRepository.saveSettings(settings))
            .thenReturn(Result.Success(Unit))

        // Act
        val result = useCase(settings)

        // Assert
        assertTrue(result is Result.Success)
        verify(settingsRepository).saveSettings(settings)
    }

    @Test
    fun `invoke should return error when settings invalid`() = runBlocking {
        // Arrange
        val settings = Settings("123", 30) // 不正な郵便番号

        // Act
        val result = useCase(settings)

        // Assert
        assertTrue(result is Result.Error)
    }
}
```

#### 5.1.2 SettingsTest

```kotlin
package com.tinygc.asachiru.domain.entity

import org.junit.Assert.*
import org.junit.Test

class SettingsTest {

    @Test
    fun `isValid should return true for valid settings`() {
        // Arrange
        val settings = Settings("1000001", 30)

        // Act
        val result = settings.isValid()

        // Assert
        assertTrue(result)
    }

    @Test
    fun `isValid should return false for invalid postal code`() {
        // Arrange
        val settings = Settings("123", 30)

        // Act
        val result = settings.isValid()

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isValid should return false for invalid news interval`() {
        // Arrange
        val settings = Settings("1000001", 100)

        // Act
        val result = settings.isValid()

        // Assert
        assertFalse(result)
    }
}
```

---

## 6. 画面遷移フロー

```
[SplashActivity]
     |
     |--checkSettings()
     |
     |--hasSettings? NO
     |         |
     |         v
     |   [SetupActivity]
     |         |
     |         |--ユーザー入力
     |         |--saveSettings()
     |         |
     |         v
     |   [MainActivity]
     |
     |--hasSettings? YES
     |         |
     |         v
     |   [MainActivity]
```

---

## 7. ファイル構成

```
domain/
├── entity/
│   └── Settings.kt
├── repository/
│   └── SettingsRepository.kt
└── usecase/
    └── settings/
        ├── SaveSettingsUseCase.kt
        ├── GetSettingsUseCase.kt
        └── CheckSettingsExistUseCase.kt

data/
├── repository/
│   └── SettingsRepositoryImpl.kt
└── datasource/
    └── local/
        └── SettingsLocalDataSource.kt

presentation/
├── splash/
│   ├── SplashActivity.kt
│   └── SplashViewModel.kt
└── setup/
    ├── SetupActivity.kt
    ├── SetupViewModel.kt
    └── SetupUiState.kt
```

---

## 8. 承認

- 作成日: 2025-11-06
- 作成者: Claude
- バージョン: 1.0

---

**設計工程完了:**
すべてのモジュールの設計書が完成しました。次はレビューを実施します。
