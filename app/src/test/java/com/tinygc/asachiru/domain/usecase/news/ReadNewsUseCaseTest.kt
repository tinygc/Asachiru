package com.tinygc.asachiru.domain.usecase.news

import com.tinygc.asachiru.domain.common.ITtsManager
import com.tinygc.asachiru.domain.entity.News
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class ReadNewsUseCaseTest {

    @Mock
    private lateinit var ttsManager: ITtsManager

    private lateinit var useCase: ReadNewsUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        useCase = ReadNewsUseCase(ttsManager)
    }

    @Test
    fun `invoke should call onNewsChanged for each news item`() = runTest {
        // Arrange
        val newsList = listOf(
            News("1", "ニュース1", "説明1", 1000L),
            News("2", "ニュース2", "説明2", 2000L)
        )
        var newsChangedCount = 0
        val changedNews = mutableListOf<News>()

        // Act
        useCase(
            newsList = newsList,
            onNewsChanged = { news ->
                newsChangedCount++
                changedNews.add(news)
            },
            onComplete = {}
        )

        // Assert
        assertEquals(2, newsChangedCount)
        assertEquals(newsList, changedNews)
    }

    @Test
    fun `invoke should call onComplete after reading all news`() = runTest {
        // Arrange
        val newsList = listOf(
            News("1", "ニュース1", "説明1", 1000L)
        )
        var onCompleteWasCalled = false

        // Act
        useCase(
            newsList = newsList,
            onNewsChanged = {},
            onComplete = { onCompleteWasCalled = true }
        )

        // Assert
        assertTrue(onCompleteWasCalled)
    }

    @Test
    fun `invoke should speak news in correct order`() = runTest {
        // Arrange
        val newsList = listOf(
            News("1", "ニュース1", "説明1", 1000L),
            News("2", "ニュース2", "説明2", 2000L),
            News("3", "ニュース3", "説明3", 3000L)
        )

        // Act
        useCase(
            newsList = newsList,
            onNewsChanged = {},
            onComplete = {}
        )

        // Assert
        // すべてのニュースの前に時刻アナウンス
        val inOrder = inOrder(ttsManager)
        // 1つ目のニュース（「次は、」なし）
        inOrder.verify(ttsManager).speak(org.mockito.kotlin.argThat { contains("時") && contains("分のニュースです。") && !contains("次は、") })
        inOrder.verify(ttsManager).waitUntilDone()
        inOrder.verify(ttsManager).speak("ニュース1。説明1")
        inOrder.verify(ttsManager).waitUntilDone()
        // 2つ目のニュース（「次は、」あり）
        inOrder.verify(ttsManager).speak(org.mockito.kotlin.argThat { contains("次は、") && contains("時") && contains("分のニュースです。") })
        inOrder.verify(ttsManager).waitUntilDone()
        inOrder.verify(ttsManager).speak("ニュース2。説明2")
        inOrder.verify(ttsManager).waitUntilDone()
        // 3つ目のニュース（「次は、」あり）
        inOrder.verify(ttsManager).speak(org.mockito.kotlin.argThat { contains("次は、") && contains("時") && contains("分のニュースです。") })
        inOrder.verify(ttsManager).waitUntilDone()
        inOrder.verify(ttsManager).speak("ニュース3。説明3")
        inOrder.verify(ttsManager).waitUntilDone()
    }

    @Test
    fun `invoke should wait until done after each speak call`() = runTest {
        // Arrange
        val newsList = listOf(
            News("1", "ニュース1", "説明1", 1000L)
        )

        // Act
        useCase(
            newsList = newsList,
            onNewsChanged = {},
            onComplete = {}
        )

        // Assert
        // 時刻アナウンス1回 + ニュース1回 = 2回のspeak/waitUntilDone
        verify(ttsManager, times(2)).speak(org.mockito.kotlin.any())
        verify(ttsManager, times(2)).waitUntilDone()
    }

    @Test
    fun `invoke should handle empty news list`() = runTest {
        // Arrange
        val emptyList = emptyList<News>()
        var onCompleteWasCalled = false

        // Act
        useCase(
            newsList = emptyList,
            onNewsChanged = {},
            onComplete = { onCompleteWasCalled = true }
        )

        // Assert
        assertTrue(onCompleteWasCalled)
        verify(ttsManager, never()).speak(org.mockito.kotlin.any())
        verify(ttsManager, never()).waitUntilDone()
    }

    @Test
    fun `invoke should use getSpeechText for TTS`() = runTest {
        // Arrange
        val news = News("1", "タイトル", "説明文", 1000L)
        val newsList = listOf(news)

        // Act
        useCase(
            newsList = newsList,
            onNewsChanged = {},
            onComplete = {}
        )

        // Assert
        // 時刻アナウンス + ニュースの2回speak()が呼ばれる
        verify(ttsManager).speak(org.mockito.kotlin.argThat { contains("時") && contains("分のニュースです。") })
        verify(ttsManager).speak("タイトル。説明文")
    }

    @Test
    fun `invoke should process multiple news items sequentially`() = runTest {
        // Arrange
        val newsList = listOf(
            News("1", "ニュース1", "内容1", 1000L),
            News("2", "ニュース2", "内容2", 2000L),
            News("3", "ニュース3", "内容3", 3000L),
            News("4", "ニュース4", "内容4", 4000L),
            News("5", "ニュース5", "内容5", 5000L)
        )
        val processedNews = mutableListOf<News>()

        // Act
        useCase(
            newsList = newsList,
            onNewsChanged = { news -> processedNews.add(news) },
            onComplete = {}
        )

        // Assert
        assertEquals(5, processedNews.size)
        assertEquals(newsList, processedNews)
        // 5つのニュース + 5回の時刻アナウンス（すべてのニュースの前） = 10回のspeak/waitUntilDone
        verify(ttsManager, times(10)).speak(org.mockito.kotlin.any())
        verify(ttsManager, times(10)).waitUntilDone()
    }

    @Test
    fun `invoke should call callbacks in correct order`() = runTest {
        // Arrange
        val news = News("1", "ニュース", "説明", 1000L)
        val newsList = listOf(news)
        val callOrder = mutableListOf<String>()

        // Act
        useCase(
            newsList = newsList,
            onNewsChanged = { callOrder.add("newsChanged") },
            onComplete = { callOrder.add("complete") }
        )

        // Assert
        assertEquals(listOf("newsChanged", "complete"), callOrder)
    }

    @Test
    fun `invoke should handle news with special characters in text`() = runTest {
        // Arrange
        val news = News(
            id = "1",
            title = "特殊文字「」『』・：",
            description = "改行\nタブ\t含む",
            publishedAt = 1000L
        )
        val newsList = listOf(news)

        // Act
        useCase(
            newsList = newsList,
            onNewsChanged = {},
            onComplete = {}
        )

        // Assert
        verify(ttsManager).speak("特殊文字「」『』・：。改行\nタブ\t含む")
    }

    @Test
    fun `invoke should announce time before all news items`() = runTest {
        // Arrange
        val newsList = listOf(
            News("1", "ニュース1", "説明1", 1000L),
            News("2", "ニュース2", "説明2", 2000L)
        )

        // Act
        useCase(
            newsList = newsList,
            onNewsChanged = {},
            onComplete = {}
        )

        // Assert
        // すべてのニュースの前に時刻アナウンス
        val inOrder = inOrder(ttsManager)
        // 1つ目のニュース（「次は、」なし）
        inOrder.verify(ttsManager).speak(org.mockito.kotlin.argThat {
            contains("時") && contains("分のニュースです。") && !contains("次は、")
        })
        inOrder.verify(ttsManager).waitUntilDone()
        inOrder.verify(ttsManager).speak("ニュース1。説明1")
        inOrder.verify(ttsManager).waitUntilDone()
        // 2つ目のニュース（「次は、」あり）
        inOrder.verify(ttsManager).speak(org.mockito.kotlin.argThat {
            contains("次は、") && contains("時") && contains("分のニュースです。")
        })
        inOrder.verify(ttsManager).waitUntilDone()
        inOrder.verify(ttsManager).speak("ニュース2。説明2")
        inOrder.verify(ttsManager).waitUntilDone()
    }

    @Test
    fun `invoke should announce time without prefix for first news item`() = runTest {
        // Arrange
        val newsList = listOf(
            News("1", "ニュース1", "説明1", 1000L)
        )

        // Act
        useCase(
            newsList = newsList,
            onNewsChanged = {},
            onComplete = {}
        )

        // Assert
        // 1つ目のニュースは「次は、」なしの時刻アナウンス
        val inOrder = inOrder(ttsManager)
        inOrder.verify(ttsManager).speak(org.mockito.kotlin.argThat {
            contains("時") && contains("分のニュースです。") && !contains("次は、")
        })
        inOrder.verify(ttsManager).waitUntilDone()
        inOrder.verify(ttsManager).speak("ニュース1。説明1")
        inOrder.verify(ttsManager).waitUntilDone()
    }

    @Test
    fun `invoke should announce time in JST timezone`() = runTest {
        // Arrange
        val newsList = listOf(
            News("1", "ニュース1", "説明1", 1000L),
            News("2", "ニュース2", "説明2", 2000L)
        )

        // Act
        useCase(
            newsList = newsList,
            onNewsChanged = {},
            onComplete = {}
        )

        // Assert
        // 時刻アナウンスのフォーマットが正しいことを確認
        verify(ttsManager).speak(org.mockito.kotlin.argThat {
            matches(Regex("次は、\\d{1,2}時\\d{1,2}分のニュースです。"))
        })
    }
}
