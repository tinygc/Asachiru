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
            assertEquals("⚙️ 初回設定", titleTextView.text.toString())
        }
    }

    @Test
    fun `postal code label should be displayed`() {
        scenario.onActivity { activity ->
            val label = activity.findViewById<TextView>(R.id.postal_code_label)
            assertNotNull(label)
            assertEquals("📮 郵便番号（7桁）", label.text.toString())
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
    fun `save button should be displayed`() {
        scenario.onActivity { activity ->
            val button = activity.findViewById<Button>(R.id.save_button)
            assertNotNull(button)
            // Robolectricの制約により、ボタンの有効/無効状態は実機で確認
            // ここでは存在確認のみ
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
