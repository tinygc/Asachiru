# モジュール設計書 - RSSフィード複数選択機能

## 1. 概要

複数のRSSフィードを選択してニュースを取得できる機能を追加するっちゃ！現在は単一フィードのみ対応だけど、チェックボックス形式で複数選択できるようにする。

---

## 2. 機能要件（再掲）

### 2.1 機能
- プリセットから複数のRSSフィード選択
- カスタムURLも複数追加可能
- 最低1つ以上のRSS選択を必須
- 選択された全フィードからニュース取得
- 公開日時でソート（新しい順）

### 2.2 UI/UX
- チェックボックス形式でプリセット一覧表示
- カスタムURL追加ボタン
- 配信元（フィード名）を記事表示時に表示

---

## 3. アーキテクチャ設計

### 3.1 レイヤー構成

```
[Presentation Layer]
  - SetupActivity (複数RSS選択UI)
  - SetupViewModel (複数RSS状態管理)
  - SetupUiState (複数RSS状態)

[Domain Layer]
  - SaveSettingsUseCase (複数RSS保存)
  - GetNewsUseCase (複数フィード取得)
  - Settings (複数RSS対応)
  - RssFeed (Entity)

[Data Layer]
  - SettingsRepositoryImpl (複数RSS永続化)
  - NewsRepositoryImpl (複数フィード並列取得)
```

---

## 4. データモデル設計

### 4.1 RssFeed Entity

```kotlin
package com.tinygc.asachiru.domain.entity

/**
 * RSSフィードを表すエンティティ
 */
data class RssFeed(
    val id: String,          // 一意なID（プリセット名 or UUID）
    val name: String,        // 表示名（プリセット名 or カスタム名）
    val url: String,         // RSS URL
    val isCustom: Boolean    // カスタムURLかどうか
) {
    companion object {
        /**
         * プリセットからRssFeedを生成
         */
        fun fromPreset(presetName: String, url: String): RssFeed {
            return RssFeed(
                id = presetName,
                name = presetName,
                url = url,
                isCustom = false
            )
        }

        /**
         * カスタムURLからRssFeedを生成
         */
        fun fromCustomUrl(url: String, customName: String? = null): RssFeed {
            return RssFeed(
                id = java.util.UUID.randomUUID().toString(),
                name = customName ?: url,
                url = url,
                isCustom = true
            )
        }
    }
}
```

### 4.2 Settings Entity（変更後）

```kotlin
package com.tinygc.asachiru.domain.entity

/**
 * 設定を表すエンティティ
 */
data class Settings(
    val postalCode: String,
    val rssFeeds: List<RssFeed>,  // 複数RSS対応
    val enableTts: Boolean,
    val enableBgm: Boolean
) {
    /**
     * 設定が有効かチェック
     */
    fun isValid(): Boolean {
        return isPostalCodeValid() && isRssFeedsValid()
    }

    /**
     * 郵便番号が有効かチェック
     */
    fun isPostalCodeValid(): Boolean {
        return postalCode.length == 7 && postalCode.all { it.isDigit() }
    }

    /**
     * RSSフィードが有効かチェック
     */
    fun isRssFeedsValid(): Boolean {
        return rssFeeds.isNotEmpty() && rssFeeds.all { it.url.isNotBlank() }
    }

    companion object {
        /**
         * デフォルト設定
         */
        val DEFAULT = Settings(
            postalCode = "",
            rssFeeds = emptyList(),
            enableTts = false,
            enableBgm = true
        )
    }
}
```

### 4.3 NewsDto（配信元追加）

```kotlin
package com.tinygc.asachiru.data.dto

import com.tinygc.asachiru.domain.entity.News
import java.util.Date

/**
 * ニュース記事のDTO
 */
data class NewsDto(
    val title: String,
    val link: String,
    val pubDate: Date,
    val sourceName: String? = null  // 配信元名（追加）
) {
    fun toEntity(): News {
        return News(
            title = title,
            link = link,
            pubDate = pubDate,
            sourceName = sourceName
        )
    }
}
```

---

## 5. クラス設計

### 5.1 Presentation Layer

#### 5.1.1 SetupUiState（変更）

```kotlin
package com.tinygc.asachiru.presentation.setup

import com.tinygc.asachiru.domain.entity.RssFeed

/**
 * 設定画面のUI状態
 */
data class SetupUiState(
    val postalCode: String = "",
    val selectedRssFeeds: List<RssFeed> = emptyList(),  // 複数RSS
    val customRssFeeds: List<RssFeed> = emptyList(),   // カスタムURL
    val enableTts: Boolean = false,
    val enableBgm: Boolean = true,
    
    val isPostalCodeValid: Boolean = true,
    val isRssFeedsValid: Boolean = true,  // 1つ以上選択されているか
    
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val saveError: String? = null
)
```

#### 5.1.2 SetupViewModel（変更）

```kotlin
package com.tinygc.asachiru.presentation.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinygc.asachiru.domain.entity.RssFeed
import com.tinygc.asachiru.domain.entity.Settings
import com.tinygc.asachiru.domain.usecase.settings.SaveSettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SetupViewModel(
    private val saveSettingsUseCase: SaveSettingsUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState

    /**
     * 郵便番号更新
     */
    fun updatePostalCode(postalCode: String) {
        _uiState.value = _uiState.value.copy(
            postalCode = postalCode,
            isPostalCodeValid = validatePostalCode(postalCode)
        )
    }

    /**
     * RSSフィード選択切り替え
     */
    fun toggleRssFeed(feed: RssFeed) {
        val current = _uiState.value.selectedRssFeeds
        val updated = if (current.any { it.id == feed.id }) {
            current.filter { it.id != feed.id }
        } else {
            current + feed
        }
        _uiState.value = _uiState.value.copy(
            selectedRssFeeds = updated,
            isRssFeedsValid = updated.isNotEmpty()
        )
    }

    /**
     * カスタムRSS追加
     */
    fun addCustomRss(url: String, name: String? = null) {
        val customFeed = RssFeed.fromCustomUrl(url, name)
        _uiState.value = _uiState.value.copy(
            customRssFeeds = _uiState.value.customRssFeeds + customFeed,
            selectedRssFeeds = _uiState.value.selectedRssFeeds + customFeed,
            isRssFeedsValid = true
        )
    }

    /**
     * カスタムRSS削除
     */
    fun removeCustomRss(feedId: String) {
        _uiState.value = _uiState.value.copy(
            customRssFeeds = _uiState.value.customRssFeeds.filter { it.id != feedId },
            selectedRssFeeds = _uiState.value.selectedRssFeeds.filter { it.id != feedId }
        )
    }

    /**
     * 設定保存
     */
    fun saveSettings() {
        val state = _uiState.value
        if (!state.isPostalCodeValid || !state.isRssFeedsValid) {
            return
        }

        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            try {
                val settings = Settings(
                    postalCode = state.postalCode,
                    rssFeeds = state.selectedRssFeeds,
                    enableTts = state.enableTts,
                    enableBgm = state.enableBgm
                )
                saveSettingsUseCase(settings)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveError = e.message
                )
            }
        }
    }

    private fun validatePostalCode(postalCode: String): Boolean {
        return postalCode.length == 7 && postalCode.all { it.isDigit() }
    }
}
```

#### 5.1.3 SetupActivity UI変更

```xml
<!-- レイアウト概要 -->
<ScrollView>
    <LinearLayout>
        <!-- 郵便番号入力 -->
        <EditText android:id="@+id/postalCodeEditText" />

        <!-- RSSフィード選択（チェックボックスリスト） -->
        <TextView android:text="RSSフィード選択（複数可）" />
        <RecyclerView 
            android:id="@+id/rssPresetsRecyclerView"
            android:layout_height="wrap_content" />

        <!-- カスタムURL追加 -->
        <Button 
            android:id="@+id/addCustomRssButton"
            android:text="カスタムURL追加" />

        <!-- カスタムRSSリスト -->
        <RecyclerView 
            android:id="@+id/customRssRecyclerView"
            android:layout_height="wrap_content" />

        <!-- TTS/BGM設定 -->
        <CheckBox android:id="@+id/enableTtsCheckbox" />
        <CheckBox android:id="@+id/enableBgmCheckbox" />

        <!-- 保存ボタン -->
        <Button android:id="@+id/saveButton" />
    </LinearLayout>
</ScrollView>
```

---

## 6. Domain Layer

### 6.1 GetNewsUseCase（変更）

```kotlin
package com.tinygc.asachiru.domain.usecase.news

import com.tinygc.asachiru.domain.entity.News
import com.tinygc.asachiru.domain.entity.RssFeed
import com.tinygc.asachiru.domain.repository.NewsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * ニュース取得UseCase（複数RSS対応）
 */
class GetNewsUseCase(
    private val newsRepository: NewsRepository
) {
    /**
     * 複数のRSSフィードからニュースを取得
     * 
     * @param rssFeeds 取得対象のRSSフィードリスト
     * @return 公開日時順にソートされたニュース一覧
     */
    suspend operator fun invoke(rssFeeds: List<RssFeed>): Result<List<News>> {
        return try {
            // 複数フィードを並列取得
            val newsList = coroutineScope {
                rssFeeds.map { feed ->
                    async {
                        newsRepository.fetchNews(feed.url, feed.name)
                            .getOrNull() ?: emptyList()
                    }
                }.awaitAll().flatten()
            }

            // 公開日時でソート（新しい順）
            val sortedNews = newsList.sortedByDescending { it.pubDate }

            // 重複削除（タイトルとURLで判定）
            val uniqueNews = sortedNews.distinctBy { "${it.title}_${it.link}" }

            Result.success(uniqueNews)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## 7. Data Layer

### 7.1 SettingsRepositoryImpl（変更）

```kotlin
package com.tinygc.asachiru.data.repository

import com.tinygc.asachiru.data.local.SettingsLocalDataSource
import com.tinygc.asachiru.domain.entity.RssFeed
import com.tinygc.asachiru.domain.entity.Settings
import com.tinygc.asachiru.domain.repository.SettingsRepository

class SettingsRepositoryImpl(
    private val localDataSource: SettingsLocalDataSource
) : SettingsRepository {

    override suspend fun saveSettings(settings: Settings) {
        // RssFeedリストをJSON化して保存
        val rssFeedsJson = serializeRssFeeds(settings.rssFeeds)
        localDataSource.saveRssFeeds(rssFeedsJson)
        localDataSource.savePostalCode(settings.postalCode)
        localDataSource.saveEnableTts(settings.enableTts)
        localDataSource.saveEnableBgm(settings.enableBgm)
    }

    override suspend fun getSettings(): Settings? {
        val postalCode = localDataSource.getPostalCode() ?: return null
        val rssFeedsJson = localDataSource.getRssFeeds() ?: return null
        val rssFeeds = deserializeRssFeeds(rssFeedsJson)
        
        return Settings(
            postalCode = postalCode,
            rssFeeds = rssFeeds,
            enableTts = localDataSource.getEnableTts(),
            enableBgm = localDataSource.getEnableBgm()
        )
    }

    private fun serializeRssFeeds(feeds: List<RssFeed>): String {
        // GsonでJSON化
        return gson.toJson(feeds)
    }

    private fun deserializeRssFeeds(json: String): List<RssFeed> {
        // Gsonでデシリアライズ
        return gson.fromJson(json, Array<RssFeed>::class.java).toList()
    }
}
```

### 7.2 NewsRepositoryImpl（変更）

```kotlin
package com.tinygc.asachiru.data.repository

import com.tinygc.asachiru.data.remote.NewsRemoteDataSource
import com.tinygc.asachiru.domain.entity.News
import com.tinygc.asachiru.domain.repository.NewsRepository

class NewsRepositoryImpl(
    private val remoteDataSource: NewsRemoteDataSource
) : NewsRepository {

    /**
     * 指定URLからニュース取得（配信元名付き）
     */
    override suspend fun fetchNews(url: String, sourceName: String): Result<List<News>> {
        return try {
            val newsDto = remoteDataSource.fetchNews(url)
            // DTOにsourceNameを設定してEntityに変換
            val newsList = newsDto.map { dto ->
                dto.copy(sourceName = sourceName).toEntity()
            }
            Result.success(newsList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## 8. データマイグレーション

既存の単一RSS設定から複数RSS設定への移行処理。

```kotlin
/**
 * 旧設定から新設定へのマイグレーション
 */
suspend fun migrateSettings() {
    val oldRssUrl = localDataSource.getOldRssUrl()
    val oldRssPreset = localDataSource.getOldRssPreset()
    
    if (oldRssUrl != null && oldRssPreset != null) {
        // 旧設定を複数RSS形式に変換
        val rssFeed = if (oldRssPreset == "カスタムURL入力") {
            RssFeed.fromCustomUrl(oldRssUrl)
        } else {
            RssFeed.fromPreset(oldRssPreset, oldRssUrl)
        }
        
        // 新形式で保存
        val settings = Settings(
            postalCode = localDataSource.getPostalCode() ?: "",
            rssFeeds = listOf(rssFeed),
            enableTts = localDataSource.getEnableTts(),
            enableBgm = localDataSource.getEnableBgm()
        )
        saveSettings(settings)
        
        // 旧設定削除
        localDataSource.deleteOldRssUrl()
        localDataSource.deleteOldRssPreset()
    }
}
```

---

## 9. UI設計詳細

### 9.1 RSSプリセットアイテム（CheckBox）

```xml
<!-- item_rss_preset_checkbox.xml -->
<CheckBox
    android:id="@+id/rssPresetCheckbox"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="NHK"
    android:textSize="18sp"
    android:padding="16dp" />
```

### 9.2 カスタムRSSアイテム

```xml
<!-- item_custom_rss.xml -->
<LinearLayout orientation="horizontal">
    <TextView
        android:id="@+id/customRssName"
        android:text="カスタムRSS 1"
        android:textSize="16sp" />
    <TextView
        android:id="@+id/customRssUrl"
        android:text="https://example.com/rss"
        android:textSize="14sp" />
    <ImageButton
        android:id="@+id/deleteButton"
        android:src="@drawable/ic_delete"
        android:contentDescription="削除" />
</LinearLayout>
```

---

## 10. エラーハンドリング

### 10.1 一部フィード取得失敗時

```kotlin
// 取得に失敗したフィード名をリストアップ
val failedFeeds = mutableListOf<String>()
rssFeeds.forEach { feed ->
    val result = newsRepository.fetchNews(feed.url, feed.name)
    if (result.isFailure) {
        failedFeeds.add(feed.name)
    }
}

// エラーメッセージ表示
if (failedFeeds.isNotEmpty()) {
    showError("以下のフィードの取得に失敗しました: ${failedFeeds.joinToString(", ")}")
}
```

---

## 11. テスト設計

### 11.1 単体テスト

```kotlin
class SetupViewModelTest {
    @Test
    fun `複数RSS選択テスト`() {
        val feed1 = RssFeed.fromPreset("NHK", "https://nhk.jp/rss")
        val feed2 = RssFeed.fromPreset("Yahoo", "https://yahoo.co.jp/rss")
        
        viewModel.toggleRssFeed(feed1)
        viewModel.toggleRssFeed(feed2)
        
        val state = viewModel.uiState.value
        assertEquals(2, state.selectedRssFeeds.size)
        assertTrue(state.isRssFeedsValid)
    }
}
```

---

## 12. 実装順序

1. **Phase 1**: データモデル変更
   - RssFeed Entity作成
   - Settings Entity変更
   - NewsDto変更

2. **Phase 2**: Repository・UseCase変更
   - SettingsRepositoryImpl変更
   - NewsRepositoryImpl変更
   - GetNewsUseCase変更

3. **Phase 3**: ViewModel・UI State変更
   - SetupUiState変更
   - SetupViewModel変更

4. **Phase 4**: UI実装
   - レイアウトXML作成
   - RecyclerViewアダプタ実装
   - SetupActivity変更

5. **Phase 5**: テスト
   - 単体テスト作成
   - E2Eテスト実行

---

## 13. 非機能要件

### 13.1 パフォーマンス
- 5個のフィードを並列取得した場合、3秒以内に完了
- メモリ使用量は増加を最小限に

### 13.2 UX
- チェックボックスのタップ領域は十分に大きく（48dp以上）
- 選択されたRSS数を表示（例: "3個選択中"）

---

## 14. 備考
- asachiru/feedwatch両フレーバー対応
- マイグレーション処理はアプリ起動時に自動実行
- 将来的にRSS並び替え機能も検討
