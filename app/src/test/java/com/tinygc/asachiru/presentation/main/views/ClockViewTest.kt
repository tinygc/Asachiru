package com.tinygc.asachiru.presentation.main.views

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.tinygc.asachiru.domain.entity.DateTime
import com.tinygc.asachiru.domain.entity.DayOfWeek
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertNotNull

/**
 * ClockViewのUIテスト
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ClockViewTest {

    private lateinit var context: Context
    private lateinit var clockView: ClockView

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        clockView = ClockView(context)
    }

    @Test
    fun `ClockView should be created successfully`() {
        // Given & When
        val view = ClockView(context)

        // Then
        assertNotNull(view)
    }

    @Test
    fun `updateDateTime should not throw exception`() {
        // Given
        val dateTime = DateTime(2025, 11, 6, 14, 30, 0, DayOfWeek.WEDNESDAY)

        // When & Then (例外が発生しないことを確認)
        clockView.updateDateTime(dateTime)
    }

    @Test
    fun `updateDateTime with EMPTY should not throw exception`() {
        // When & Then (例外が発生しないことを確認)
        clockView.updateDateTime(DateTime.EMPTY)
    }

    @Test
    fun `updateDateTime can be called multiple times`() {
        // Given
        val dateTime1 = DateTime(2025, 11, 6, 14, 30, 0, DayOfWeek.WEDNESDAY)
        val dateTime2 = DateTime(2025, 11, 6, 14, 31, 0, DayOfWeek.WEDNESDAY)
        val dateTime3 = DateTime(2025, 11, 6, 14, 32, 0, DayOfWeek.WEDNESDAY)

        // When & Then (例外が発生しないことを確認)
        clockView.updateDateTime(dateTime1)
        clockView.updateDateTime(dateTime2)
        clockView.updateDateTime(dateTime3)
    }

    @Test
    fun `updateDateTime with Sunday should not throw exception`() {
        // Given
        val dateTime = DateTime(2025, 11, 9, 10, 0, 0, DayOfWeek.SUNDAY)

        // When & Then (例外が発生しないことを確認)
        clockView.updateDateTime(dateTime)
    }

    @Test
    fun `updateDateTime with Saturday should not throw exception`() {
        // Given
        val dateTime = DateTime(2025, 11, 8, 10, 0, 0, DayOfWeek.SATURDAY)

        // When & Then (例外が発生しないことを確認)
        clockView.updateDateTime(dateTime)
    }

    @Test
    fun `updateDateTime with Monday should not throw exception`() {
        // Given
        val dateTime = DateTime(2025, 11, 10, 10, 0, 0, DayOfWeek.MONDAY)

        // When & Then (例外が発生しないことを確認)
        clockView.updateDateTime(dateTime)
    }

    @Test
    fun `updateDateTime with Tuesday should not throw exception`() {
        // Given
        val dateTime = DateTime(2025, 11, 11, 10, 0, 0, DayOfWeek.TUESDAY)

        // When & Then (例外が発生しないことを確認)
        clockView.updateDateTime(dateTime)
    }

    @Test
    fun `updateDateTime with Thursday should not throw exception`() {
        // Given
        val dateTime = DateTime(2025, 11, 13, 10, 0, 0, DayOfWeek.THURSDAY)

        // When & Then (例外が発生しないことを確認)
        clockView.updateDateTime(dateTime)
    }

    @Test
    fun `updateDateTime with Friday should not throw exception`() {
        // Given
        val dateTime = DateTime(2025, 11, 14, 10, 0, 0, DayOfWeek.FRIDAY)

        // When & Then (例外が発生しないことを確認)
        clockView.updateDateTime(dateTime)
    }

    @Test
    fun `updateDateTime with midnight time should not throw exception`() {
        // Given
        val dateTime = DateTime(2025, 11, 6, 0, 0, 0, DayOfWeek.WEDNESDAY)

        // When & Then (例外が発生しないことを確認)
        clockView.updateDateTime(dateTime)
    }

    @Test
    fun `updateDateTime with 23_59 time should not throw exception`() {
        // Given
        val dateTime = DateTime(2025, 11, 6, 23, 59, 59, DayOfWeek.WEDNESDAY)

        // When & Then (例外が発生しないことを確認)
        clockView.updateDateTime(dateTime)
    }

    @Test
    fun `updateDateTime with single digit month and day should not throw exception`() {
        // Given
        val dateTime = DateTime(2025, 1, 5, 9, 8, 7, DayOfWeek.MONDAY)

        // When & Then (例外が発生しないことを確認)
        clockView.updateDateTime(dateTime)
    }

    @Test
    fun `updateDateTime with different years should not throw exception`() {
        // Given
        val dateTime1 = DateTime(2024, 11, 6, 14, 30, 0, DayOfWeek.WEDNESDAY)
        val dateTime2 = DateTime(2025, 11, 6, 14, 30, 0, DayOfWeek.WEDNESDAY)
        val dateTime3 = DateTime(2026, 11, 6, 14, 30, 0, DayOfWeek.WEDNESDAY)

        // When & Then (例外が発生しないことを確認)
        clockView.updateDateTime(dateTime1)
        clockView.updateDateTime(dateTime2)
        clockView.updateDateTime(dateTime3)
    }

    @Test
    fun `ClockView with AttributeSet should be created successfully`() {
        // When
        val view = ClockView(context, null)

        // Then
        assertNotNull(view)
    }

    @Test
    fun `ClockView with all constructor parameters should be created successfully`() {
        // When
        val view = ClockView(context, null, 0)

        // Then
        assertNotNull(view)
    }

    @Test
    fun `updateDateTime after EMPTY should not throw exception`() {
        // Given
        clockView.updateDateTime(DateTime.EMPTY)
        val dateTime = DateTime(2025, 11, 6, 14, 30, 0, DayOfWeek.WEDNESDAY)

        // When & Then (例外が発生しないことを確認)
        clockView.updateDateTime(dateTime)
    }

    @Test
    fun `updateDateTime with leap year February 29 should not throw exception`() {
        // Given
        val dateTime = DateTime(2024, 2, 29, 12, 0, 0, DayOfWeek.THURSDAY)

        // When & Then (例外が発生しないことを確認)
        clockView.updateDateTime(dateTime)
    }

    @Test
    fun `updateDateTime with December 31 should not throw exception`() {
        // Given
        val dateTime = DateTime(2025, 12, 31, 23, 59, 59, DayOfWeek.WEDNESDAY)

        // When & Then (例外が発生しないことを確認)
        clockView.updateDateTime(dateTime)
    }
}
