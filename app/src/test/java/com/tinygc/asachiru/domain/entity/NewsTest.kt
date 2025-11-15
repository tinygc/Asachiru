package com.tinygc.asachiru.domain.entity

import org.junit.Assert.*
import org.junit.Test

class NewsTest {

    @Test
    fun `News should hold all news information`() {
        // Arrange & Act
        val news = News(
            id = "news-001",
            title = "テストニュースタイトル",
            description = "テストニュースの説明文です",
            publishedAt = 1699257600000L
        )

        // Assert
        assertEquals("news-001", news.id)
        assertEquals("テストニュースタイトル", news.title)
        assertEquals("テストニュースの説明文です", news.description)
        assertEquals(1699257600000L, news.publishedAt)
    }

    @Test
    fun `getSpeechText should combine title and description`() {
        // Arrange
        val news = News(
            id = "news-002",
            title = "台風情報",
            description = "台風が接近しています",
            publishedAt = 1699257600000L
        )

        // Act
        val speechText = news.getSpeechText()

        // Assert
        assertEquals("台風情報。台風が接近しています", speechText)
    }

    @Test
    fun `getSpeechText should work with empty description`() {
        // Arrange
        val news = News(
            id = "news-003",
            title = "速報",
            description = "",
            publishedAt = 1699257600000L
        )

        // Act
        val speechText = news.getSpeechText()

        // Assert
        assertEquals("速報。", speechText)
    }

    @Test
    fun `EMPTY should have default values`() {
        // Act
        val empty = News.EMPTY

        // Assert
        assertEquals("", empty.id)
        assertEquals("", empty.title)
        assertEquals("", empty.description)
        assertEquals(0L, empty.publishedAt)
    }

    @Test
    fun `News should be data class with correct equality`() {
        // Arrange
        val news1 = News(
            id = "news-001",
            title = "タイトル",
            description = "説明",
            publishedAt = 1699257600000L
        )
        val news2 = News(
            id = "news-001",
            title = "タイトル",
            description = "説明",
            publishedAt = 1699257600000L
        )
        val news3 = News(
            id = "news-002",
            title = "別のタイトル",
            description = "別の説明",
            publishedAt = 1699257700000L
        )

        // Assert
        assertEquals(news1, news2)
        assertNotEquals(news1, news3)
    }

    @Test
    fun `News should handle long text correctly`() {
        // Arrange
        val longTitle = "これは非常に長いニュースタイトルです。" + "文字が続きます。".repeat(10)
        val longDescription = "これは非常に長い説明文です。" + "内容が続きます。".repeat(20)

        val news = News(
            id = "news-long",
            title = longTitle,
            description = longDescription,
            publishedAt = 1699257600000L
        )

        // Act
        val speechText = news.getSpeechText()

        // Assert
        assertTrue(speechText.contains(longTitle))
        assertTrue(speechText.contains(longDescription))
        assertTrue(speechText.contains("。"))
    }

    @Test
    fun `News publishedAt should handle different timestamp values`() {
        // Arrange
        val timestamps = listOf(
            0L,
            1699257600000L,
            System.currentTimeMillis(),
            Long.MAX_VALUE
        )

        // Act & Assert
        timestamps.forEach { timestamp ->
            val news = News(
                id = "test",
                title = "test",
                description = "test",
                publishedAt = timestamp
            )
            assertEquals(timestamp, news.publishedAt)
        }
    }
}
