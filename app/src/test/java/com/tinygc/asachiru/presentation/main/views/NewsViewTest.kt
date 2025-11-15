package com.tinygc.asachiru.presentation.main.views

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.tinygc.asachiru.domain.entity.News
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertNotNull

/**
 * NewsViewのUIテスト
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NewsViewTest {

    private lateinit var context: Context
    private lateinit var newsView: NewsView

    private val news1 = News(
        id = "https://example.com/news1",
        title = "テストニュース1",
        description = "テスト説明1",
        publishedAt = 1000L
    )

    private val news2 = News(
        id = "https://example.com/news2",
        title = "重要なニュース速報",
        description = "テスト説明2",
        publishedAt = 2000L
    )

    private val longTitleNews = News(
        id = "https://example.com/news3",
        title = "非常に長いタイトルのニュース".repeat(10),
        description = "テスト説明3",
        publishedAt = 3000L
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        newsView = NewsView(context)
    }

    @Test
    fun `NewsView should be created successfully`() {
        // Given & When
        val view = NewsView(context)

        // Then
        assertNotNull(view)
    }

    @Test
    fun `updateNews should not throw exception`() {
        // When & Then (例外が発生しないことを確認)
        newsView.updateNews(news1)
    }

    @Test
    fun `updateNews with null should not throw exception`() {
        // When & Then (例外が発生しないことを確認)
        newsView.updateNews(null)
    }

    @Test
    fun `showError should not throw exception`() {
        // Given
        val errorMessage = "ニュース取得エラー"

        // When & Then (例外が発生しないことを確認)
        newsView.showError(errorMessage)
    }

    @Test
    fun `updateNews can be called multiple times`() {
        // When & Then (例外が発生しないことを確認)
        newsView.updateNews(news1)
        newsView.updateNews(news2)
        newsView.updateNews(longTitleNews)
    }

    @Test
    fun `showError can be called multiple times`() {
        // When & Then (例外が発生しないことを確認)
        newsView.showError("エラー1")
        newsView.showError("エラー2")
        newsView.showError("エラー3")
    }

    @Test
    fun `updateNews after showError should not throw exception`() {
        // Given
        newsView.showError("エラー")

        // When & Then (例外が発生しないことを確認)
        newsView.updateNews(news1)
    }

    @Test
    fun `showError after updateNews should not throw exception`() {
        // Given
        newsView.updateNews(news1)

        // When & Then (例外が発生しないことを確認)
        newsView.showError("エラー")
    }

    @Test
    fun `updateNews with long title should not throw exception`() {
        // When & Then (例外が発生しないことを確認)
        newsView.updateNews(longTitleNews)
    }

    @Test
    fun `updateNews with empty title should not throw exception`() {
        // Given
        val emptyTitleNews = News(
            id = "https://example.com/news4",
            title = "",
            description = "テスト説明4",
            publishedAt = 4000L
        )

        // When & Then (例外が発生しないことを確認)
        newsView.updateNews(emptyTitleNews)
    }

    @Test
    fun `showError with empty message should not throw exception`() {
        // When & Then (例外が発生しないことを確認)
        newsView.showError("")
    }

    @Test
    fun `showError with long message should not throw exception`() {
        // Given
        val longMessage = "エラー".repeat(100)

        // When & Then (例外が発生しないことを確認)
        newsView.showError(longMessage)
    }

    @Test
    fun `NewsView with AttributeSet should be created successfully`() {
        // When
        val view = NewsView(context, null)

        // Then
        assertNotNull(view)
    }

    @Test
    fun `NewsView with all constructor parameters should be created successfully`() {
        // When
        val view = NewsView(context, null, 0)

        // Then
        assertNotNull(view)
    }

    @Test
    fun `updateNews alternating with null should not throw exception`() {
        // When & Then (例外が発生しないことを確認)
        newsView.updateNews(news1)
        newsView.updateNews(null)
        newsView.updateNews(news2)
        newsView.updateNews(null)
    }

    @Test
    fun `updateNews with special characters in title should not throw exception`() {
        // Given
        val specialCharNews = News(
            id = "https://example.com/news5",
            title = "特殊文字!@#$%^&*()_+-={}[]|:;<>?,./",
            description = "テスト説明5",
            publishedAt = 5000L
        )

        // When & Then (例外が発生しないことを確認)
        newsView.updateNews(specialCharNews)
    }

    @Test
    fun `updateNews with Unicode emoji in title should not throw exception`() {
        // Given
        val emojiNews = News(
            id = "https://example.com/news6",
            title = "😀😁😂🤣😃😄😅😆😉",
            description = "テスト説明6",
            publishedAt = 6000L
        )

        // When & Then (例外が発生しないことを確認)
        newsView.updateNews(emojiNews)
    }

    @Test
    fun `updateNews with Japanese characters should not throw exception`() {
        // Given
        val japaneseNews = News(
            id = "https://example.com/news7",
            title = "日本語のニュースタイトル",
            description = "テスト説明7",
            publishedAt = 7000L
        )

        // When & Then (例外が発生しないことを確認)
        newsView.updateNews(japaneseNews)
    }

    @Test
    fun `updateNews with English title should not throw exception`() {
        // Given
        val englishNews = News(
            id = "https://example.com/news8",
            title = "Breaking News: Important Event",
            description = "Test description 8",
            publishedAt = 8000L
        )

        // When & Then (例外が発生しないことを確認)
        newsView.updateNews(englishNews)
    }

    @Test
    fun `updateNews with mixed language title should not throw exception`() {
        // Given
        val mixedNews = News(
            id = "https://example.com/news9",
            title = "ニュース News 新闻 새소식",
            description = "テスト説明9",
            publishedAt = 9000L
        )

        // When & Then (例外が発生しないことを確認)
        newsView.updateNews(mixedNews)
    }
}
