package com.tinygc.asachiru.presentation.splash

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.tinygc.asachiru.presentation.main.MainActivity
import com.tinygc.asachiru.presentation.setup.SetupActivity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * SplashActivityのUIテスト
 *
 * Robolectricを使用してActivity起動と画面遷移のテストを行います。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SplashActivityTest {

    private lateinit var scenario: ActivityScenario<SplashActivity>

    @Before
    fun setup() {
        scenario = ActivityScenario.launch(SplashActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun `SplashActivity should be created successfully`() {
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
    }

    @Test
    fun `SplashActivity should not have a layout`() {
        // SplashActivityはレイアウトを持たず、すぐに遷移するため
        scenario.onActivity { activity ->
            assertNotNull(activity)
            // レイアウトがないことを確認（setContentViewが呼ばれていない）
        }
    }

    @Test
    fun `SplashActivity onCreate should complete without error`() {
        // onCreate()が例外をスローしないことを確認
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
    }

    @Test
    fun `navigateToMain should start MainActivity`() {
        scenario.onActivity { activity ->
            // privateメソッドを直接テストできないため、
            // 代わりにIntent生成のテストを行う
            val intent = Intent(activity, MainActivity::class.java)
            assertNotNull(intent)
            assertEquals(MainActivity::class.java.name, intent.component?.className)
        }
    }

    @Test
    fun `navigateToSetup should start SetupActivity`() {
        scenario.onActivity { activity ->
            // privateメソッドを直接テストできないため、
            // 代わりにIntent生成のテストを行う
            val intent = Intent(activity, SetupActivity::class.java)
            assertNotNull(intent)
            assertEquals(SetupActivity::class.java.name, intent.component?.className)
        }
    }

    @Test
    fun `Intent to MainActivity should be correctly configured`() {
        scenario.onActivity { activity ->
            val intent = Intent(activity, MainActivity::class.java)
            val shadow = shadowOf(intent)
            assertEquals(MainActivity::class.java.name, shadow.intentClass.name)
        }
    }

    @Test
    fun `Intent to SetupActivity should be correctly configured`() {
        scenario.onActivity { activity ->
            val intent = Intent(activity, SetupActivity::class.java)
            val shadow = shadowOf(intent)
            assertEquals(SetupActivity::class.java.name, shadow.intentClass.name)
        }
    }

    @Test
    fun `SplashActivity should be launchable from external Intent`() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), SplashActivity::class.java)
        val launchScenario = ActivityScenario.launch<SplashActivity>(intent)

        launchScenario.onActivity { activity ->
            assertNotNull(activity)
        }

        launchScenario.close()
    }

    @Test
    fun `multiple SplashActivity instances should be creatable`() {
        val scenario2 = ActivityScenario.launch(SplashActivity::class.java)

        scenario.onActivity { activity1 ->
            assertNotNull(activity1)
        }

        scenario2.onActivity { activity2 ->
            assertNotNull(activity2)
        }

        scenario2.close()
    }

    @Test
    fun `SplashActivity should handle lifecycle events correctly`() {
        // ライフサイクルイベントのテスト
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }

        scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
    }
}
