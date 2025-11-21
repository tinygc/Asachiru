package com.tinygc.asachiru.domain.usecase.news

import com.tinygc.asachiru.domain.common.AppException
import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.News
import com.tinygc.asachiru.domain.entity.NewsResult
import com.tinygc.asachiru.domain.repository.NewsRepository
import com.tinygc.asachiru.domain.repository.ReadArticleRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class GetLatestNewsUseCaseTest {

    @Mock
    private lateinit var newsRepository: NewsRepository

    @Mock
    private lateinit var readArticleRepository: ReadArticleRepository

    private lateinit var useCase: GetLatestNewsUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        useCase = GetLatestNewsUseCase(newsRepository, readArticleRepository)
    }

    @Test
    fun `invoke should return news list when repository returns success`() = runTest {
        // Arrange
        val expectedNews = listOf(
            News("1", "タイトル1", "説明1", 1000L),
            News("2", "タイトル2", "説明2", 2000L)
        )

        whenever(readArticleRepository.getReadArticleIds())
            .thenReturn(emptySet())
        whenever(newsRepository.getLatestNews(10))
            .thenReturn(Result.Success(expectedNews))

        // Act
        val result = useCase(10)

        // Assert
        assertTrue(result is Result.Success)
        val newsResult = (result as Result.Success).data
        assertEquals(expectedNews, newsResult.allArticles)
        assertEquals(expectedNews, newsResult.newArticles)
    }

    @Test
    fun `invoke should return error when repository returns error`() = runTest {
        // Arrange
        val exception = AppException.NetworkException("Network error")

        whenever(readArticleRepository.getReadArticleIds())
            .thenReturn(emptySet())
        whenever(newsRepository.getLatestNews(10))
            .thenReturn(Result.Error(exception))

        // Act
        val result = useCase(10)

        // Assert
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }

    @Test
    fun `invoke should use default count of 10 when no parameter provided`() = runTest {
        // Arrange
        val expectedNews = listOf(
            News("1", "タイトル1", "説明1", 1000L)
        )

        whenever(readArticleRepository.getReadArticleIds())
            .thenReturn(emptySet())
        whenever(newsRepository.getLatestNews(10))
            .thenReturn(Result.Success(expectedNews))

        // Act
        val result = useCase() // No parameter, should use default 10

        // Assert
        assertTrue(result is Result.Success)
        val newsResult = (result as Result.Success).data
        assertEquals(expectedNews, newsResult.allArticles)
        assertEquals(expectedNews, newsResult.newArticles)
    }

    @Test
    fun `invoke should handle different count values`() = runTest {
        // Arrange
        val counts = listOf(1, 5, 10, 20, 50)

        counts.forEach { count ->
            val expectedNews = (1..count).map { i ->
                News("$i", "タイトル$i", "説明$i", i * 1000L)
            }

            whenever(readArticleRepository.getReadArticleIds())
                .thenReturn(emptySet())
            whenever(newsRepository.getLatestNews(count))
                .thenReturn(Result.Success(expectedNews))

            // Act
            val result = useCase(count)

            // Assert
            assertTrue("Result should be Success for count $count", result is Result.Success)
            val newsResult = (result as Result.Success).data
            assertEquals(count, newsResult.allArticles.size)
            assertEquals(count, newsResult.newArticles.size)
        }
    }

    @Test
    fun `invoke should return error when repository throws exception`() = runTest {
        // Arrange
        val exception = RuntimeException("Unexpected error")
        whenever(readArticleRepository.getReadArticleIds())
            .thenReturn(emptySet())
        whenever(newsRepository.getLatestNews(10)).thenThrow(exception)

        // Act
        val result = useCase(10)

        // Assert
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }

    @Test
    fun `invoke should handle empty news list`() = runTest {
        // Arrange
        val emptyList = emptyList<News>()
        whenever(readArticleRepository.getReadArticleIds())
            .thenReturn(emptySet())
        whenever(newsRepository.getLatestNews(10))
            .thenReturn(Result.Success(emptyList))

        // Act
        val result = useCase(10)

        // Assert
        assertTrue(result is Result.Success)
        val newsResult = (result as Result.Success).data
        assertTrue(newsResult.allArticles.isEmpty())
        assertTrue(newsResult.newArticles.isEmpty())
    }

    @Test
    fun `invoke should handle network exception from repository`() = runTest {
        // Arrange
        val networkException = AppException.NetworkException("Connection timeout")
        whenever(readArticleRepository.getReadArticleIds())
            .thenReturn(emptySet())
        whenever(newsRepository.getLatestNews(10))
            .thenReturn(Result.Error(networkException))

        // Act
        val result = useCase(10)

        // Assert
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception is AppException.NetworkException)
        assertEquals("Connection timeout", error.exception.message)
    }

    @Test
    fun `invoke should handle parse exception from repository`() = runTest {
        // Arrange
        val parseException = AppException.ParseException("Failed to parse RSS")
        whenever(readArticleRepository.getReadArticleIds())
            .thenReturn(emptySet())
        whenever(newsRepository.getLatestNews(10))
            .thenReturn(Result.Error(parseException))

        // Act
        val result = useCase(10)

        // Assert
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception is AppException.ParseException)
        assertEquals("Failed to parse RSS", error.exception.message)
    }

    @Test
    fun `invoke should return news with correct data structure`() = runTest {
        // Arrange
        val news = News(
            id = "https://example.com/news/123",
            title = "重要なニュース",
            description = "詳細な説明文です",
            publishedAt = 1699257600000L
        )
        val expectedNews = listOf(news)

        whenever(readArticleRepository.getReadArticleIds())
            .thenReturn(emptySet())
        whenever(newsRepository.getLatestNews(10))
            .thenReturn(Result.Success(expectedNews))

        // Act
        val result = useCase(10)

        // Assert
        assertTrue(result is Result.Success)
        val newsResult = (result as Result.Success).data
        val actualNews = newsResult.allArticles.first()
        assertEquals("https://example.com/news/123", actualNews.id)
        assertEquals("重要なニュース", actualNews.title)
        assertEquals("詳細な説明文です", actualNews.description)
        assertEquals(1699257600000L, actualNews.publishedAt)
    }
}
