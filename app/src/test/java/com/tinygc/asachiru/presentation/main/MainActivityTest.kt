package com.tinygc.asachiru.presentation.main

import com.tinygc.asachiru.presentation.main.views.ClockView
import com.tinygc.asachiru.presentation.main.views.NewsView
import com.tinygc.asachiru.presentation.main.views.VisualizerView
import com.tinygc.asachiru.presentation.main.views.WeatherView
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertNotNull

/**
 * MainActivityのUIテスト
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MainActivityTest {

    private lateinit var activity: MainActivity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .start()
            .resume()
            .get()
    }

    @Test
    fun `MainActivity should be created successfully`() {
        // Then
        assertNotNull(activity)
    }

    @Test
    fun `MainActivity should have ClockView`() {
        // When
        val clockView = activity.findViewById<ClockView>(
            activity.resources.getIdentifier("clock_view", "id", activity.packageName)
        )

        // Then
        assertNotNull(clockView)
    }

    @Test
    fun `MainActivity should have WeatherView`() {
        // When
        val weatherView = activity.findViewById<WeatherView>(
            activity.resources.getIdentifier("weather_view", "id", activity.packageName)
        )

        // Then
        assertNotNull(weatherView)
    }

    @Test
    fun `MainActivity should have NewsView`() {
        // When
        val newsView = activity.findViewById<NewsView>(
            activity.resources.getIdentifier("news_view", "id", activity.packageName)
        )

        // Then
        assertNotNull(newsView)
    }

    @Test
    fun `MainActivity should have VisualizerView`() {
        // When
        val visualizerView = activity.findViewById<VisualizerView>(
            activity.resources.getIdentifier("visualizer_view", "id", activity.packageName)
        )

        // Then
        assertNotNull(visualizerView)
    }

    @Test
    fun `MainActivity onCreate should not throw exception`() {
        // Given & When
        val testActivity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .get()

        // Then
        assertNotNull(testActivity)
    }

    @Test
    fun `MainActivity onResume should not throw exception`() {
        // Given
        val testActivity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .start()

        // When & Then (例外が発生しないことを確認)
        testActivity.resume()
    }

    @Test
    fun `MainActivity lifecycle should work correctly`() {
        // Given
        val testActivity = Robolectric.buildActivity(MainActivity::class.java)

        // When & Then (例外が発生しないことを確認)
        testActivity.create()
        testActivity.start()
        testActivity.resume()
        testActivity.pause()
        testActivity.stop()
        testActivity.destroy()
    }

    @Test
    fun `MainActivity can be recreated`() {
        // Given
        val testActivity1 = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .start()
            .resume()
            .get()

        // When
        val testActivity2 = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .start()
            .resume()
            .get()

        // Then
        assertNotNull(testActivity1)
        assertNotNull(testActivity2)
    }
}
