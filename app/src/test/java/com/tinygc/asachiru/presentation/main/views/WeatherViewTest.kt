package com.tinygc.asachiru.presentation.main.views

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.tinygc.asachiru.domain.entity.Weather
import com.tinygc.asachiru.domain.entity.WeatherCondition
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertNotNull

/**
 * WeatherViewのUIテスト
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class WeatherViewTest {

    private lateinit var context: Context
    private lateinit var weatherView: WeatherView

    private val sunnyWeather = Weather(
        condition = WeatherCondition.SUNNY,
        currentTemperature = 25,
        maxTemperature = 28,
        minTemperature = 18,
        precipitationProbability = 10,
        dateLabel = "今日"
    )

    private val cloudyWeather = Weather(
        condition = WeatherCondition.CLOUDY,
        currentTemperature = 20,
        maxTemperature = 22,
        minTemperature = 15,
        precipitationProbability = 30,
        dateLabel = "今日"
    )

    private val rainyWeather = Weather(
        condition = WeatherCondition.RAINY,
        currentTemperature = 18,
        maxTemperature = 20,
        minTemperature = 14,
        precipitationProbability = 80,
        dateLabel = "今日"
    )

    private val snowyWeather = Weather(
        condition = WeatherCondition.SNOWY,
        currentTemperature = -2,
        maxTemperature = 0,
        minTemperature = -5,
        precipitationProbability = 90,
        dateLabel = "今日"
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        weatherView = WeatherView(context)
    }

    @Test
    fun `WeatherView should be created successfully`() {
        // Given & When
        val view = WeatherView(context)

        // Then
        assertNotNull(view)
    }

    @Test
    fun `updateWeather should not throw exception with sunny weather`() {
        // When & Then (例外が発生しないことを確認)
        weatherView.updateWeather(sunnyWeather)
    }

    @Test
    fun `updateWeather should not throw exception with cloudy weather`() {
        // When & Then (例外が発生しないことを確認)
        weatherView.updateWeather(cloudyWeather)
    }

    @Test
    fun `updateWeather should not throw exception with rainy weather`() {
        // When & Then (例外が発生しないことを確認)
        weatherView.updateWeather(rainyWeather)
    }

    @Test
    fun `updateWeather should not throw exception with snowy weather`() {
        // When & Then (例外が発生しないことを確認)
        weatherView.updateWeather(snowyWeather)
    }

    @Test
    fun `updateWeather should not throw exception with null`() {
        // When & Then (例外が発生しないことを確認)
        weatherView.updateWeather(null)
    }

    @Test
    fun `showError should not throw exception`() {
        // Given
        val errorMessage = "ネットワークエラー"

        // When & Then (例外が発生しないことを確認)
        weatherView.showError(errorMessage)
    }

    @Test
    fun `updateWeather can be called multiple times`() {
        // When & Then (例外が発生しないことを確認)
        weatherView.updateWeather(sunnyWeather)
        weatherView.updateWeather(cloudyWeather)
        weatherView.updateWeather(rainyWeather)
        weatherView.updateWeather(snowyWeather)
    }

    @Test
    fun `showError can be called multiple times`() {
        // When & Then (例外が発生しないことを確認)
        weatherView.showError("エラー1")
        weatherView.showError("エラー2")
        weatherView.showError("エラー3")
    }

    @Test
    fun `updateWeather after showError should not throw exception`() {
        // Given
        weatherView.showError("エラー")

        // When & Then (例外が発生しないことを確認)
        weatherView.updateWeather(sunnyWeather)
    }

    @Test
    fun `showError after updateWeather should not throw exception`() {
        // Given
        weatherView.updateWeather(sunnyWeather)

        // When & Then (例外が発生しないことを確認)
        weatherView.showError("エラー")
    }

    @Test
    fun `updateWeather with extreme temperatures should not throw exception`() {
        // Given
        val extremeWeather = Weather(
            condition = WeatherCondition.SNOWY,
            currentTemperature = -40,
            maxTemperature = -30,
            minTemperature = -50,
            precipitationProbability = 100,
            dateLabel = "今日"
        )

        // When & Then (例外が発生しないことを確認)
        weatherView.updateWeather(extremeWeather)
    }

    @Test
    fun `updateWeather with high temperatures should not throw exception`() {
        // Given
        val hotWeather = Weather(
            condition = WeatherCondition.SUNNY,
            currentTemperature = 40,
            maxTemperature = 42,
            minTemperature = 35,
            precipitationProbability = 0,
            dateLabel = "今日"
        )

        // When & Then (例外が発生しないことを確認)
        weatherView.updateWeather(hotWeather)
    }

    @Test
    fun `updateWeather with zero precipitation should not throw exception`() {
        // Given
        val dryWeather = Weather(
            condition = WeatherCondition.SUNNY,
            currentTemperature = 25,
            maxTemperature = 28,
            minTemperature = 20,
            precipitationProbability = 0,
            dateLabel = "今日"
        )

        // When & Then (例外が発生しないことを確認)
        weatherView.updateWeather(dryWeather)
    }

    @Test
    fun `updateWeather with 100% precipitation should not throw exception`() {
        // Given
        val wetWeather = Weather(
            condition = WeatherCondition.RAINY,
            currentTemperature = 15,
            maxTemperature = 18,
            minTemperature = 12,
            precipitationProbability = 100,
            dateLabel = "今日"
        )

        // When & Then (例外が発生しないことを確認)
        weatherView.updateWeather(wetWeather)
    }

    @Test
    fun `showError with empty message should not throw exception`() {
        // When & Then (例外が発生しないことを確認)
        weatherView.showError("")
    }

    @Test
    fun `showError with long message should not throw exception`() {
        // Given
        val longMessage = "エラー".repeat(100)

        // When & Then (例外が発生しないことを確認)
        weatherView.showError(longMessage)
    }

    @Test
    fun `WeatherView with AttributeSet should be created successfully`() {
        // When
        val view = WeatherView(context, null)

        // Then
        assertNotNull(view)
    }

    @Test
    fun `WeatherView with all constructor parameters should be created successfully`() {
        // When
        val view = WeatherView(context, null, 0)

        // Then
        assertNotNull(view)
    }

    @Test
    fun `updateWeather with same temperature values should not throw exception`() {
        // Given
        val sameTemperatureWeather = Weather(
            condition = WeatherCondition.CLOUDY,
            currentTemperature = 20,
            maxTemperature = 20,
            minTemperature = 20,
            precipitationProbability = 50,
            dateLabel = "今日"
        )

        // When & Then (例外が発生しないことを確認)
        weatherView.updateWeather(sameTemperatureWeather)
    }

    @Test
    fun `updateWeather alternating with null should not throw exception`() {
        // When & Then (例外が発生しないことを確認)
        weatherView.updateWeather(sunnyWeather)
        weatherView.updateWeather(null)
        weatherView.updateWeather(rainyWeather)
        weatherView.updateWeather(null)
    }
}
