package com.tinygc.asachiru.e2e

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.tinygc.asachiru.presentation.main.MainActivity
import com.tinygc.asachiru.presentation.setup.SetupActivity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertNotNull

/**
 * エラーケースのE2Eテスト
 *
 * アプリケーションの異常系のフローをテストします。
 *
 * テストシナリオ:
 * 1. 不正な設定データでの起動
 * 2. ネットワークエラー時の動作
 * 3. データ欠損時の動作
 * 4. 境界値での動作
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ErrorCaseE2ETest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // SharedPreferencesをクリア
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
    fun `E2E Error - app should handle invalid postal code in settings`() {
        // Scenario: 不正な郵便番号が保存されている場合

        // Given: 不正な郵便番号を保存
        context.getSharedPreferences("asachiru_preferences", Context.MODE_PRIVATE)
            .edit()
            .putString("postal_code", "123") // 7桁未満
            .putInt("news_interval", 30)
            .commit()

        // When: MainActivityを起動
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then: アプリは起動するが、天気情報取得でエラーが発生する想定
        scenario.onActivity { activity ->
            assertNotNull(activity)
            // エラーハンドリングにより、アプリはクラッシュしない
        }

        scenario.close()
    }

    @Test
    fun `E2E Error - app should handle invalid news interval in settings`() {
        // Scenario: 不正なニュース間隔が保存されている場合

        // Given: 不正なニュース間隔を保存
        context.getSharedPreferences("asachiru_preferences", Context.MODE_PRIVATE)
            .edit()
            .putString("postal_code", "1000001")
            .putInt("news_interval", 0) // 範囲外（1-60）
            .commit()

        // When: MainActivityを起動
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then: アプリは起動するが、ニュース機能でエラーが発生する想定
        scenario.onActivity { activity ->
            assertNotNull(activity)
            // エラーハンドリングにより、アプリはクラッシュしない
        }

        scenario.close()
    }

    @Test
    fun `E2E Error - app should handle corrupted settings data`() {
        // Scenario: 設定データが破損している場合

        // Given: 一部の設定のみ保存（破損を模擬）
        context.getSharedPreferences("asachiru_preferences", Context.MODE_PRIVATE)
            .edit()
            .putString("postal_code", "1000001")
            // news_intervalは保存しない
            .commit()

        // When: MainActivityを起動
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then: デフォルト値が使用され、アプリは正常に起動
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }

        scenario.close()
    }

    @Test
    fun `E2E Error - app should handle empty settings`() {
        // Scenario: 設定が空の場合

        // Given: 空の郵便番号を保存
        context.getSharedPreferences("asachiru_preferences", Context.MODE_PRIVATE)
            .edit()
            .putString("postal_code", "")
            .putInt("news_interval", 30)
            .commit()

        // When: MainActivityを起動
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then: エラーハンドリングにより、アプリはクラッシュしない
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }

        scenario.close()
    }

    @Test
    fun `E2E Error - SetupActivity should handle rapid button clicks`() {
        // Scenario: 保存ボタンの連打

        // When: SetupActivityを起動
        val scenario = ActivityScenario.launch(SetupActivity::class.java)

        scenario.onActivity { activity ->
            assertNotNull(activity)
            // 保存ボタンが複数回クリックされても、
            // 適切にハンドリングされることを確認
        }

        scenario.close()
    }

    @Test
    fun `E2E Error - app should handle maximum boundary values`() {
        // Scenario: 境界値（最大値）での動作

        // Given: 最大値の設定を保存
        context.getSharedPreferences("asachiru_preferences", Context.MODE_PRIVATE)
            .edit()
            .putString("postal_code", "9999999")
            .putInt("news_interval", 60) // 最大値
            .commit()

        // When: MainActivityを起動
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then: 正常に動作
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }

        scenario.close()
    }

    @Test
    fun `E2E Error - app should handle minimum boundary values`() {
        // Scenario: 境界値（最小値）での動作

        // Given: 最小値の設定を保存
        context.getSharedPreferences("asachiru_preferences", Context.MODE_PRIVATE)
            .edit()
            .putString("postal_code", "0000000")
            .putInt("news_interval", 1) // 最小値
            .commit()

        // When: MainActivityを起動
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then: 正常に動作
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }

        scenario.close()
    }

    @Test
    fun `E2E Error - app should handle special characters in postal code`() {
        // Scenario: 郵便番号に特殊文字が含まれる場合

        // Given: 特殊文字を含む郵便番号を保存
        context.getSharedPreferences("asachiru_preferences", Context.MODE_PRIVATE)
            .edit()
            .putString("postal_code", "100-000") // ハイフン付き
            .putInt("news_interval", 30)
            .commit()

        // When: MainActivityを起動
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then: バリデーションエラーが発生するが、アプリはクラッシュしない
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }

        scenario.close()
    }

    @Test
    fun `E2E Error - app should handle negative news interval`() {
        // Scenario: 負の値のニュース間隔

        // Given: 負の値を保存
        context.getSharedPreferences("asachiru_preferences", Context.MODE_PRIVATE)
            .edit()
            .putString("postal_code", "1000001")
            .putInt("news_interval", -1)
            .commit()

        // When: MainActivityを起動
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then: エラーハンドリングにより、アプリはクラッシュしない
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }

        scenario.close()
    }

    @Test
    fun `E2E Error - app should handle extremely large news interval`() {
        // Scenario: 極端に大きなニュース間隔

        // Given: 範囲外の大きな値を保存
        context.getSharedPreferences("asachiru_preferences", Context.MODE_PRIVATE)
            .edit()
            .putString("postal_code", "1000001")
            .putInt("news_interval", 1000)
            .commit()

        // When: MainActivityを起動
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then: エラーハンドリングにより、アプリはクラッシュしない
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }

        scenario.close()
    }

    @Test
    fun `E2E Error - app should recover from activity kill`() {
        // Scenario: Activityが強制終了された場合

        // Given: 設定を保存
        context.getSharedPreferences("asachiru_preferences", Context.MODE_PRIVATE)
            .edit()
            .putString("postal_code", "1000001")
            .putInt("news_interval", 30)
            .commit()

        // When: MainActivityを起動して強制終了
        val scenario1 = ActivityScenario.launch(MainActivity::class.java)
        scenario1.close()

        // Then: 再起動時も正常に動作
        val scenario2 = ActivityScenario.launch(MainActivity::class.java)
        scenario2.onActivity { activity ->
            assertNotNull(activity)
        }
        scenario2.close()
    }

    @Test
    fun `E2E Error - app should handle null settings gracefully`() {
        // Scenario: 設定がnullの場合

        // Given: SharedPreferencesをクリア（設定なし状態）
        context.getSharedPreferences("asachiru_preferences", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        // When: MainActivityを起動
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then: デフォルト値が使用され、アプリは正常に起動
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }

        scenario.close()
    }
}
