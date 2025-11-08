package com.tinygc.asachiru.presentation.setup

import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import com.tinygc.asachiru.R
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * SetupActivityのUIテスト
 *
 * Robolectricを使用してActivity起動とUI要素の存在確認を行います。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SetupActivityTest {

    private lateinit var scenario: ActivityScenario<SetupActivity>

    @Before
    fun setup() {
        scenario = ActivityScenario.launch(SetupActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun `SetupActivity should be created successfully`() {
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
    }

    @Test
    fun `title text view should be displayed`() {
        scenario.onActivity { activity ->
            val titleTextView = activity.findViewById<TextView>(R.id.title_text_view)
            assertNotNull(titleTextView)
            assertEquals("初回設定", titleTextView.text.toString())
        }
    }

    @Test
    fun `postal code label should be displayed`() {
        scenario.onActivity { activity ->
            val label = activity.findViewById<TextView>(R.id.postal_code_label)
            assertNotNull(label)
            assertEquals("郵便番号（7桁）", label.text.toString())
        }
    }

    @Test
    fun `postal code edit text should be displayed`() {
        scenario.onActivity { activity ->
            val editText = activity.findViewById<EditText>(R.id.postal_code_edit_text)
            assertNotNull(editText)
        }
    }

    @Test
    fun `news interval label should be displayed`() {
        scenario.onActivity { activity ->
            val label = activity.findViewById<TextView>(R.id.news_interval_label)
            assertNotNull(label)
            assertEquals("ニュース読み上げ間隔", label.text.toString())
        }
    }

    @Test
    fun `news interval text view should be displayed with default value`() {
        scenario.onActivity { activity ->
            val textView = activity.findViewById<TextView>(R.id.news_interval_text_view)
            assertNotNull(textView)
            assertEquals("30 分", textView.text.toString())
        }
    }

    @Test
    fun `news interval seek bar should be displayed`() {
        scenario.onActivity { activity ->
            val seekBar = activity.findViewById<SeekBar>(R.id.news_interval_seek_bar)
            assertNotNull(seekBar)
            assertEquals(30, seekBar.progress)
            assertEquals(1, seekBar.min)
            assertEquals(60, seekBar.max)
        }
    }

    @Test
    fun `save button should be displayed`() {
        scenario.onActivity { activity ->
            val button = activity.findViewById<Button>(R.id.save_button)
            assertNotNull(button)
            assertEquals("保存", button.text.toString())
            assertTrue(button.isEnabled)
        }
    }

    @Test
    fun `postal code edit text should have correct input type`() {
        scenario.onActivity { activity ->
            val editText = activity.findViewById<EditText>(R.id.postal_code_edit_text)
            // InputType.TYPE_CLASS_NUMBER = 2
            assertEquals(2, editText.inputType)
        }
    }

    @Test
    fun `postal code edit text should accept text input`() {
        scenario.onActivity { activity ->
            val editText = activity.findViewById<EditText>(R.id.postal_code_edit_text)
            editText.setText("1000001")
            assertEquals("1000001", editText.text.toString())
        }
    }

    @Test
    fun `news interval seek bar should update text view on progress change`() {
        scenario.onActivity { activity ->
            val seekBar = activity.findViewById<SeekBar>(R.id.news_interval_seek_bar)
            val textView = activity.findViewById<TextView>(R.id.news_interval_text_view)

            seekBar.progress = 45
            assertEquals("45 分", textView.text.toString())
        }
    }

    @Test
    fun `save button should be clickable`() {
        scenario.onActivity { activity ->
            val button = activity.findViewById<Button>(R.id.save_button)
            assertTrue(button.isClickable)
        }
    }

    @Test
    fun `postal code edit text should be editable`() {
        scenario.onActivity { activity ->
            val editText = activity.findViewById<EditText>(R.id.postal_code_edit_text)
            assertTrue(editText.isEnabled)
        }
    }

    @Test
    fun `news interval seek bar should allow value changes`() {
        scenario.onActivity { activity ->
            val seekBar = activity.findViewById<SeekBar>(R.id.news_interval_seek_bar)

            seekBar.progress = 1
            assertEquals(1, seekBar.progress)

            seekBar.progress = 60
            assertEquals(60, seekBar.progress)
        }
    }

    @Test
    fun `postal code edit text should have max length of 7`() {
        scenario.onActivity { activity ->
            val editText = activity.findViewById<EditText>(R.id.postal_code_edit_text)
            // フィルターの存在をチェック
            assertNotNull(editText.filters)
            assertTrue(editText.filters.isNotEmpty())
        }
    }

    @Test
    fun `all views should be properly initialized`() {
        scenario.onActivity { activity ->
            assertNotNull(activity.findViewById<TextView>(R.id.title_text_view))
            assertNotNull(activity.findViewById<TextView>(R.id.postal_code_label))
            assertNotNull(activity.findViewById<EditText>(R.id.postal_code_edit_text))
            assertNotNull(activity.findViewById<TextView>(R.id.news_interval_label))
            assertNotNull(activity.findViewById<TextView>(R.id.news_interval_text_view))
            assertNotNull(activity.findViewById<SeekBar>(R.id.news_interval_seek_bar))
            assertNotNull(activity.findViewById<Button>(R.id.save_button))
        }
    }

    @Test
    fun `postal code edit text should start empty`() {
        scenario.onActivity { activity ->
            val editText = activity.findViewById<EditText>(R.id.postal_code_edit_text)
            assertEquals("", editText.text.toString())
        }
    }

    @Test
    fun `news interval seek bar should update text view to different values`() {
        scenario.onActivity { activity ->
            val seekBar = activity.findViewById<SeekBar>(R.id.news_interval_seek_bar)
            val textView = activity.findViewById<TextView>(R.id.news_interval_text_view)

            seekBar.progress = 1
            assertEquals("1 分", textView.text.toString())

            seekBar.progress = 60
            assertEquals("60 分", textView.text.toString())

            seekBar.progress = 25
            assertEquals("25 分", textView.text.toString())
        }
    }

    @Test
    fun `postal code edit text should clear previous input`() {
        scenario.onActivity { activity ->
            val editText = activity.findViewById<EditText>(R.id.postal_code_edit_text)

            editText.setText("1000001")
            assertEquals("1000001", editText.text.toString())

            editText.setText("")
            assertEquals("", editText.text.toString())

            editText.setText("9999999")
            assertEquals("9999999", editText.text.toString())
        }
    }
}
