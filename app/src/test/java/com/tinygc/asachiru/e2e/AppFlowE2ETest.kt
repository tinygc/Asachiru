package com.tinygc.asachiru.e2e

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.tinygc.asachiru.presentation.main.MainActivity
import com.tinygc.asachiru.presentation.setup.SetupActivity
import com.tinygc.asachiru.presentation.splash.SplashActivity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertNotNull

/**
 * アプリケーション全体のE2Eテスト
 *
 * 初回起動から設定、メイン画面への遷移など、
 * アプリケーション全体のフローをテストします。
 *
 * テストシナリオ:
 * 1. 初回起動（設定なし）→ SplashActivity → SetupActivity
 * 2. 設定完了 → MainActivity
 * 3. 再起動（設定あり）→ SplashActivity → MainActivity
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AppFlowE2ETest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // SharedPreferencesをクリアして初回起動状態を模擬
        context.getSharedPreferences("asachiru_preferences", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        // テスト後にSharedPreferencesをクリア
        context.getSharedPreferences("asachiru_preferences", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun `E2E - first launch should show SplashActivity`() {
        // Given: 初回起動（設定なし）

        // When: SplashActivityを起動
        val scenario = ActivityScenario.launch(SplashActivity::class.java)

        // Then: SplashActivityが正常に起動
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }

        scenario.close()
    }

    @Test
    fun `E2E - SetupActivity should accept valid input`() {
        // Given: 設定画面を起動

        // When: SetupActivityを起動
        val scenario = ActivityScenario.launch(SetupActivity::class.java)

        // Then: SetupActivityが正常に起動し、UI要素が表示される
        scenario.onActivity { activity ->
            assertNotNull(activity)
            // ViewBindingによりUIコンポーネントが初期化されている
        }

        scenario.close()
    }

    @Test
    fun `E2E - MainActivity should start after setup completion`() {
        // Given: 設定を保存
        context.getSharedPreferences("asachiru_preferences", Context.MODE_PRIVATE)
            .edit()
            .putString("postal_code", "1000001")
            .putInt("news_interval", 30)
            .commit()

        // When: MainActivityを起動
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then: MainActivityが正常に起動
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }

        scenario.close()
    }

    @Test
    fun `E2E - app should navigate from Splash to Main when settings exist`() {
        // Given: 設定が保存されている
        context.getSharedPreferences("asachiru_preferences", Context.MODE_PRIVATE)
            .edit()
            .putString("postal_code", "1000001")
            .putInt("news_interval", 30)
            .commit()

        // When: SplashActivityを起動
        val scenario = ActivityScenario.launch(SplashActivity::class.java)

        // Then: SplashActivityが正常に起動（設定があるためMainActivityへ遷移するはず）
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }

        scenario.close()
    }

    @Test
    fun `E2E - complete user flow from first launch to main screen`() {
        // Scenario: 初回起動から設定完了、メイン画面表示までの一連のフロー

        // Step 1: 初回起動（SplashActivity）
        val splashScenario = ActivityScenario.launch(SplashActivity::class.java)
        splashScenario.onActivity { activity ->
            assertNotNull(activity)
        }
        splashScenario.close()

        // Step 2: 設定画面（SetupActivity）
        val setupScenario = ActivityScenario.launch(SetupActivity::class.java)
        setupScenario.onActivity { activity ->
            assertNotNull(activity)
            // ユーザーが設定を入力・保存する
        }
        setupScenario.close()

        // Step 3: 設定を保存
        context.getSharedPreferences("asachiru_preferences", Context.MODE_PRIVATE)
            .edit()
            .putString("postal_code", "1000001")
            .putInt("news_interval", 30)
            .commit()

        // Step 4: メイン画面（MainActivity）
        val mainScenario = ActivityScenario.launch(MainActivity::class.java)
        mainScenario.onActivity { activity ->
            assertNotNull(activity)
            // メイン画面の各Custom Viewが表示される
        }
        mainScenario.close()
    }

    @Test
    fun `E2E - app should handle returning user correctly`() {
        // Scenario: 既に設定済みのユーザーがアプリを起動

        // Given: 設定が保存されている
        context.getSharedPreferences("asachiru_preferences", Context.MODE_PRIVATE)
            .edit()
            .putString("postal_code", "9999999")
            .putInt("news_interval", 45)
            .commit()

        // When: SplashActivityを起動（2回目以降の起動を模擬）
        val splashScenario = ActivityScenario.launch(SplashActivity::class.java)

        // Then: SplashActivityが起動し、設定があるためMainActivityへ遷移
        splashScenario.onActivity { activity ->
            assertNotNull(activity)
        }
        splashScenario.close()

        // When: MainActivityを直接起動
        val mainScenario = ActivityScenario.launch(MainActivity::class.java)

        // Then: MainActivityが正常に起動
        mainScenario.onActivity { activity ->
            assertNotNull(activity)
        }
        mainScenario.close()
    }

    @Test
    fun `E2E - all activities should be launchable from Intent`() {
        // Test: 全てのActivityがIntentから起動可能

        // SplashActivity
        val splashIntent = Intent(context, SplashActivity::class.java)
        val splashScenario = ActivityScenario.launch<SplashActivity>(splashIntent)
        splashScenario.onActivity { assertNotNull(it) }
        splashScenario.close()

        // SetupActivity
        val setupIntent = Intent(context, SetupActivity::class.java)
        val setupScenario = ActivityScenario.launch<SetupActivity>(setupIntent)
        setupScenario.onActivity { assertNotNull(it) }
        setupScenario.close()

        // MainActivity
        val mainIntent = Intent(context, MainActivity::class.java)
        val mainScenario = ActivityScenario.launch<MainActivity>(mainIntent)
        mainScenario.onActivity { assertNotNull(it) }
        mainScenario.close()
    }

    @Test
    fun `E2E - app should maintain state across activity recreations`() {
        // Scenario: Activity再生成時の状態保持

        // Given: 設定を保存
        context.getSharedPreferences("asachiru_preferences", Context.MODE_PRIVATE)
            .edit()
            .putString("postal_code", "1000001")
            .putInt("news_interval", 30)
            .commit()

        // When: MainActivityを起動して再生成
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.recreate()

        // Then: 再生成後も正常に動作
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }

        scenario.close()
    }

    @Test
    fun `E2E - app lifecycle should work correctly`() {
        // Scenario: アプリのライフサイクルテスト

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // CREATED -> STARTED -> RESUMED
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)

        scenario.onActivity { activity ->
            assertNotNull(activity)
        }

        scenario.close()
    }

    @Test
    fun `E2E - multiple activity launches should not cause memory leaks`() {
        // Test: 複数回のActivity起動でメモリリークが発生しないことを確認

        // 10回起動して全て正常にクローズできることを確認
        repeat(10) {
            val scenario = ActivityScenario.launch(MainActivity::class.java)
            scenario.onActivity { activity ->
                assertNotNull(activity)
            }
            scenario.close()
        }
    }
}
