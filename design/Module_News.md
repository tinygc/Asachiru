# モジュール設計書 - ニュース読み上げ機能

## 1. 概要

ニュース読み上げ機能は、NHKニュースRSSから最新ニュースを取得し、Android標準TTSで読み上げる機能。

---

## 2. 機能要件（再掲）

### 2.1 基本仕様
- **ニュースソース**: NHKニュースRSS
- **音声エンジン**: Android標準TTS（Text-to-Speech）
- **読み上げ件数**: 最新10件

### 2.2 読み上げタイミング
- アプリ起動10秒後から最新10件を読み上げ開始
- その後はユーザー設定の間隔で定期的に最新10件を読み上げ

### 2.3 読み上げ間隔
- **仕様**: 固定間隔（現在、デフォルト値は30分に設定されています）
- **詳細**: 読み上げ間隔はユーザー側で設定できず、アプリケーション内部で定義された固定値が使用されます。

### 2.4 表示
- 読み上げ中のニュースタイトルを画面左下に表示

### 2.5 エラー処理
- RSS取得エラー時: 該当箇所にエラーメッセージを表示
- 他の機能は正常動作を継続

---

## 3. アーキテクチャ設計

### 3.1 レイヤー構成

```
[Presentation Layer]
  - NewsView (Custom View)
  - MainViewModel
  - TtsManager (TTSラッパー)

[Domain Layer]
  - GetLatestNewsUseCase
  - ReadNewsUseCase
  - News (Entity)
  - NewsRepository (Interface)

[Data Layer]
  - NewsRepositoryImpl
  - NewsRssDataSource
  - NewsDto
  - RssParser
```

---

## 4. クラス設計

### 4.1 Domain Layer

#### 4.1.1 News Entity

```kotlin
package com.tinygc.asachiru.domain.entity

/**
 * ニュースを表すエンティティ
 */
data class News(
    val id: String,
    val title: String,
    val description: String,
    val publishedAt: Long // Unixタイムスタンプ（ミリ秒）
) {
    /**
     * 読み上げ用テキストを取得
     */
    fun getSpeechText(): String {
        return "$title。$description"
    }

    companion object {
        /**
         * 空のNews（初期値用）
         */
        val EMPTY = News(
            id = "",
            title = "",
            description = "",
            publishedAt = 0L
        )
    }
}
```

#### 4.1.2 NewsRepository Interface

```kotlin
package com.tinygc.asachiru.domain.repository

import com.tinygc.asachiru.domain.entity.News
import com.tinygc.asachiru.domain.common.Result

/**
 * ニュース情報を取得するリポジトリのインターフェース
 */
interface NewsRepository {
    /**
     * 最新のニュースを取得
     * @param count 取得件数
     * @return ニュースリスト（Result型）
     */
    suspend fun getLatestNews(count: Int): Result<List<News>>
}
```

#### 4.1.3 GetLatestNewsUseCase

```kotlin
package com.tinygc.asachiru.domain.usecase.news

import com.tinygc.asachiru.domain.entity.News
import com.tinygc.asachiru.domain.repository.NewsRepository
import com.tinygc.asachiru.domain.common.Result

/**
 * 最新ニュースを取得するユースケース
 */
class GetLatestNewsUseCase(
    private val newsRepository: NewsRepository
) {
    /**
     * 最新ニュースを取得
     * @param count 取得件数（デフォルト: 10件）
     * @return ニュースリスト（Result型）
     */
    suspend operator fun invoke(count: Int = 10): Result<List<News>> {
        return try {
            newsRepository.getLatestNews(count)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
```

#### 4.1.4 ReadNewsUseCase

```kotlin
package com.tinygc.asachiru.domain.usecase.news

import com.tinygc.asachiru.domain.entity.News

/**
 * ニュースを読み上げるユースケース
 */
class ReadNewsUseCase(
    private val ttsManager: TtsManager
) {
    /**
     * ニュースを読み上げる
     * @param newsList 読み上げるニュースリスト
     * @param onNewsChanged 読み上げ中のニュースが変わったときのコールバック
     * @param onComplete すべての読み上げが完了したときのコールバック
     */
    suspend operator fun invoke(
        newsList: List<News>,
        onNewsChanged: (News) -> Unit,
        onComplete: () -> Unit
    ) {
        newsList.forEach { news ->
            onNewsChanged(news)
            ttsManager.speak(news.getSpeechText())
            ttsManager.waitUntilDone()
        }
        onComplete()
    }
}
```

### 4.2 Data Layer

#### 4.2.1 NewsDto

```kotlin
package com.tinygc.asachiru.data.dto

import com.tinygc.asachiru.domain.entity.News
import java.text.SimpleDateFormat
import java.util.*

/**
 * RSS ItemをパースしたDTO
 */
data class NewsDto(
    val title: String,
    val description: String,
    val link: String,
    val pubDate: String // RFC 822形式
) {
    /**
     * NewsエンティティへConverter
     */
    fun toEntity(): News {
        return News(
            id = link, // リンクをIDとして使用
            title = title,
            description = description,
            publishedAt = parsePubDate(pubDate)
        )
    }

    /**
     * pubDateをUnixタイムスタンプに変換
     */
    private fun parsePubDate(pubDate: String): Long {
        return try {
            val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
            val date = format.parse(pubDate)
            date?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
```

#### 4.2.2 RssParser

```kotlin
package com.tinygc.asachiru.data.util

import com.tinygc.asachiru.data.dto.NewsDto
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

/**
 * RSSをパースするユーティリティ
 */
object RssParser {
    /**
     * RSS XMLをパースしてNewsDtoリストを返す
     * @param inputStream RSS XMLのInputStream
     * @return NewsDtoリスト
     */
    fun parse(inputStream: InputStream): List<NewsDto> {
        val newsList = mutableListOf<NewsDto>()

        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(inputStream, null)

        var eventType = parser.eventType
        var currentTag: String? = null

        var title: String? = null
        var description: String? = null
        var link: String? = null
        var pubDate: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                }
                XmlPullParser.TEXT -> {
                    when (currentTag) {
                        "title" -> title = parser.text
                        "description" -> description = parser.text
                        "link" -> link = parser.text
                        "pubDate" -> pubDate = parser.text
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item") {
                        // itemタグの終了 → NewsDto作成
                        if (title != null && description != null && link != null && pubDate != null) {
                            newsList.add(
                                NewsDto(
                                    title = title,
                                    description = description,
                                    link = link,
                                    pubDate = pubDate
                                )
                            )
                        }
                        // リセット
                        title = null
                        description = null
                        link = null
                        pubDate = null
                    }
                }
            }
            eventType = parser.next()
        }

        return newsList
    }
}
```

#### 4.2.3 NewsRssDataSource

```kotlin
package com.tinygc.asachiru.data.datasource.remote

import com.tinygc.asachiru.data.dto.NewsDto
import com.tinygc.asachiru.data.util.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * NHKニュースRSSからデータを取得するデータソース
 */
class NewsRssDataSource(
    private val httpClient: OkHttpClient
) {
    companion object {
        // NHKニュースRSS
        private const val RSS_URL = "https://www3.nhk.or.jp/rss/news/cat0.xml"
    }

    /**
     * 最新ニュースを取得
     * @return NewsDtoリスト
     * @throws IOException ネットワークエラー
     */
    suspend fun fetchLatestNews(): List<NewsDto> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(RSS_URL)
            .get()
            .build()

        val response = httpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("HTTP Error: ${response.code}")
        }

        val inputStream = response.body?.byteStream()
            ?: throw IOException("Empty response")

        RssParser.parse(inputStream)
    }
}
```

#### 4.2.4 NewsRepositoryImpl

```kotlin
package com.tinygc.asachiru.data.repository

import com.tinygc.asachiru.data.datasource.remote.NewsRssDataSource
import com.tinygc.asachiru.data.dto.NewsDto
import com.tinygc.asachiru.domain.entity.News
import com.tinygc.asachiru.domain.repository.NewsRepository
import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.common.AppException
import java.io.IOException

/**
 * NewsRepositoryの実装
 */
class NewsRepositoryImpl(
    private val newsRssDataSource: NewsRssDataSource
) : NewsRepository {

    override suspend fun getLatestNews(count: Int): Result<List<News>> {
        return try {
            val newsDtoList = newsRssDataSource.fetchLatestNews()

            // DTO → Entity変換 & 件数制限
            val newsList = newsDtoList
                .take(count)
                .map { it.toEntity() }

            Result.Success(newsList)
        } catch (e: IOException) {
            Result.Error(AppException.NetworkException(e.message ?: "Network error"))
        } catch (e: Exception) {
            Result.Error(AppException.ParseException(e.message ?: "Failed to parse RSS"))
        }
    }
}
```

### 4.3 Presentation Layer

#### 4.3.1 TtsManager

```kotlin
package com.tinygc.asachiru.presentation.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume

/**
 * TTSを管理するマネージャークラス
 */
class TtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    private val isReady = CompletableDeferred<Boolean>()

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.JAPANESE
                isReady.complete(true)
            } else {
                isReady.complete(false)
            }
        }
    }

    /**
     * TTS初期化完了を待機
     * @return 初期化成功の場合true
     */
    private suspend fun awaitReady(): Boolean {
        return isReady.await()
    }

    /**
     * テキストを読み上げる（非同期）
     * @param text 読み上げるテキスト
     */
    suspend fun speak(text: String) {
        if (!awaitReady()) {
            // TTS初期化失敗
            return
        }

        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, text.hashCode().toString())
    }

    /**
     * 読み上げが完了するまで待機
     */
    suspend fun waitUntilDone() = suspendCancellableCoroutine<Unit> { continuation ->
        val listener = object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                continuation.resume(Unit)
            }

            override fun onError(utteranceId: String?) {
                continuation.resume(Unit)
            }
        }

        tts?.setOnUtteranceProgressListener(listener)

        continuation.invokeOnCancellation {
            tts?.stop()
        }
    }

    /**
     * TTSを停止
     */
    fun stop() {
        tts?.stop()
    }

    /**
     * リソース解放
     */
    fun shutdown() {
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
```

#### 4.3.2 MainViewModel（ニュース部分のみ抜粋）

```kotlin
package com.tinygc.asachiru.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinygc.asachiru.domain.usecase.news.GetLatestNewsUseCase
import com.tinygc.asachiru.domain.usecase.news.ReadNewsUseCase
import com.tinygc.asachiru.domain.repository.SettingsRepository
import com.tinygc.asachiru.domain.common.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(
    private val getLatestNewsUseCase: GetLatestNewsUseCase,
    private val readNewsUseCase: ReadNewsUseCase,
    private val settingsRepository: SettingsRepository,
    // ... 他のUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        startNewsReading()
        // ... 他の初期化処理
    }

    /**
     * ニュース読み上げを開始
     */
    private fun startNewsReading() {
        viewModelScope.launch {
            // 初回は10秒待機
            delay(10_000L)

            while (isActive) {
                readNews()

                // 設定された間隔で待機
                val settings = settingsRepository.getSettings()
                val intervalMs = settings.newsIntervalMinutes * 60 * 1000L
                delay(intervalMs)
            }
        }
    }

    /**
     * ニュースを読み上げる
     */
    private suspend fun readNews() {
        _uiState.update { it.copy(isNewsLoading = true) }

        when (val result = getLatestNewsUseCase(10)) {
            is Result.Success -> {
                _uiState.update {
                    it.copy(
                        isNewsLoading = false,
                        newsError = null
                    )
                }

                // 読み上げ実行
                readNewsUseCase(
                    newsList = result.data,
                    onNewsChanged = { news ->
                        _uiState.update { it.copy(currentNews = news) }
                    },
                    onComplete = {
                        _uiState.update { it.copy(currentNews = null) }
                    }
                )
            }
            is Result.Error -> {
                _uiState.update {
                    it.copy(
                        isNewsLoading = false,
                        newsError = result.exception.message
                    )
                }
            }
        }
    }
}
```

#### 4.3.3 NewsView

```kotlin
package com.tinygc.asachiru.presentation.main.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.tinygc.asachiru.domain.entity.News

/**
 * ニューステキストを表示するカスタムビュー
 */
class NewsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentNews: News? = null
    private var errorMessage: String? = null

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 20f
        color = Color.WHITE
    }

    /**
     * ニュースを更新
     */
    fun updateNews(news: News?) {
        this.currentNews = news
        this.errorMessage = null
        invalidate()
    }

    /**
     * エラーメッセージを表示
     */
    fun showError(message: String) {
        this.errorMessage = message
        this.currentNews = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (errorMessage != null) {
            drawError(canvas)
            return
        }

        currentNews?.let {
            drawNewsTitle(canvas, it)
        }
    }

    private fun drawError(canvas: Canvas) {
        textPaint.color = Color.RED
        canvas.drawText(
            "Error: $errorMessage",
            50f, height - 50f, textPaint
        )
    }

    private fun drawNewsTitle(canvas: Canvas, news: News) {
        textPaint.color = Color.WHITE
        canvas.drawText(
            "📰 ${news.title}",
            50f, height - 50f, textPaint
        )
    }
}
```

---

## 5. テスト設計

### 5.1 単体テスト

#### 5.1.1 GetLatestNewsUseCaseTest

```kotlin
package com.tinygc.asachiru.domain.usecase.news

import com.tinygc.asachiru.domain.entity.News
import com.tinygc.asachiru.domain.repository.NewsRepository
import com.tinygc.asachiru.domain.common.Result
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class GetLatestNewsUseCaseTest {

    @Mock
    private lateinit var newsRepository: NewsRepository

    private lateinit var useCase: GetLatestNewsUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        useCase = GetLatestNewsUseCase(newsRepository)
    }

    @Test
    fun `invoke should return news list when repository returns success`() = runBlocking {
        // Arrange
        val expectedNews = listOf(
            News("1", "タイトル1", "説明1", 1000L),
            News("2", "タイトル2", "説明2", 2000L)
        )

        whenever(newsRepository.getLatestNews(10))
            .thenReturn(Result.Success(expectedNews))

        // Act
        val result = useCase(10)

        // Assert
        assertTrue(result is Result.Success)
        assertEquals(expectedNews, (result as Result.Success).data)
    }

    @Test
    fun `invoke should return error when repository returns error`() = runBlocking {
        // Arrange
        val exception = Exception("Network error")

        whenever(newsRepository.getLatestNews(10))
            .thenReturn(Result.Error(exception))

        // Act
        val result = useCase(10)

        // Assert
        assertTrue(result is Result.Error)
    }
}
```

#### 5.1.2 RssParserTest

```kotlin
package com.tinygc.asachiru.data.util

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class RssParserTest {

    @Test
    fun `parse should return news list from valid RSS`() {
        // Arrange
        val rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <item>
                        <title>テストニュース1</title>
                        <description>テスト説明1</description>
                        <link>https://example.com/1</link>
                        <pubDate>Wed, 06 Nov 2025 12:00:00 +0900</pubDate>
                    </item>
                    <item>
                        <title>テストニュース2</title>
                        <description>テスト説明2</description>
                        <link>https://example.com/2</link>
                        <pubDate>Wed, 06 Nov 2025 13:00:00 +0900</pubDate>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(rssXml.toByteArray())

        // Act
        val result = RssParser.parse(inputStream)

        // Assert
        assertEquals(2, result.size)
        assertEquals("テストニュース1", result[0].title)
        assertEquals("テスト説明1", result[0].description)
    }
}
```

---

## 6. シーケンス図

```
[MainActivity] [MainViewModel] [GetLatestNewsUseCase] [NewsRepo] [ReadNewsUseCase] [TtsManager]
     |              |                   |                 |              |              |
     |--onCreate()->|                   |                 |              |              |
     |              |--init{}---------->|                 |              |              |
     |              |--delay(10s)------>|                 |              |              |
     |              |                   |                 |              |              |
     |              |--invoke()-------->|                 |              |              |
     |              |                   |--getLatestNews()->              |              |
     |              |                   |<--Result<List<News>>------------|              |
     |              |<--Result----------|                 |              |              |
     |              |                   |                 |              |              |
     |              |--invoke(newsList)-------------------->              |              |
     |              |                   |                 |              |--speak()---->|
     |              |                   |                 |              |              |--TTS--->
     |              |                   |                 |              |--waitUntilDone()->
     |              |                   |                 |              |<--done-------|
     |              |<--onNewsChanged(news)---------------|              |              |
     |              |--update uiState-->|                 |              |              |
     |<--StateFlow--|                   |                 |              |              |
     |--updateNews()->[NewsView]        |                 |              |              |
     |              |                   |                 |              |              |
     |              |   (次のニュースも同様に繰り返し)         |              |              |
```

---

## 7. ファイル構成

```
domain/
├── entity/
│   └── News.kt
├── repository/
│   └── NewsRepository.kt
└── usecase/
    └── news/
        ├── GetLatestNewsUseCase.kt
        └── ReadNewsUseCase.kt

data/
├── repository/
│   └── NewsRepositoryImpl.kt
├── datasource/
│   └── remote/
│       └── NewsRssDataSource.kt
├── dto/
│   └── NewsDto.kt
└── util/
    └── RssParser.kt

presentation/
├── util/
│   └── TtsManager.kt
└── main/
    ├── MainViewModel.kt
    └── views/
        └── NewsView.kt
```

---

## 8. 外部RSS仕様

### 8.1 NHKニュースRSS

**URL:**
```
https://www3.nhk.or.jp/rss/news/cat0.xml
```

**フォーマット:** RSS 2.0

**レスポンス例:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0">
    <channel>
        <title>NHKニュース</title>
        <item>
            <title>ニュースタイトル</title>
            <description>ニュースの説明</description>
            <link>https://www3.nhk.or.jp/news/...</link>
            <pubDate>Wed, 06 Nov 2025 12:00:00 +0900</pubDate>
        </item>
    </channel>
</rss>
```

---

## 9. 承認

- 作成日: 2025-11-06
- 作成者: Claude
- バージョン: 1.0

---

**次のステップ:**
音楽再生機能のモジュール設計書を作成する。
