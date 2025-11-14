package com.tinygc.asachiru.presentation.main.views

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * BackgroundGradientViewのUIテスト
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BackgroundGradientViewTest {

    private lateinit var context: Context
    private lateinit var backgroundGradientView: BackgroundGradientView

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        backgroundGradientView = BackgroundGradientView(context)
    }

    @Test
    fun `BackgroundGradientView should be created successfully`() {
        // Given & When
        val view = BackgroundGradientView(context)

        // Then
        assertNotNull(view)
    }

    @Test
    fun `BackgroundGradientView with AttributeSet should be created successfully`() {
        // When
        val view = BackgroundGradientView(context, null)

        // Then
        assertNotNull(view)
    }

    @Test
    fun `BackgroundGradientView with all constructor parameters should be created successfully`() {
        // When
        val view = BackgroundGradientView(context, null, 0)

        // Then
        assertNotNull(view)
    }

    @Test
    fun `multiple BackgroundGradientView instances can be created`() {
        // When
        val view1 = BackgroundGradientView(context)
        val view2 = BackgroundGradientView(context)
        val view3 = BackgroundGradientView(context)

        // Then (例外が発生しないことを確認)
        assertNotNull(view1)
        assertNotNull(view2)
        assertNotNull(view3)
    }
}
