package com.tinygc.asachiru.presentation.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.tinygc.asachiru.R
import com.tinygc.asachiru.presentation.common.ViewModelFactory
import com.tinygc.asachiru.presentation.main.MainActivity
import com.tinygc.asachiru.presentation.setup.SetupActivity
import kotlinx.coroutines.launch

/**
 * スプラッシュ画面（初回設定チェック）
 *
 * アプリ起動時に設定の存在をチェックし、適切な画面へ遷移します。
 * - 設定がある場合: MainActivity
 * - 設定がない場合: SetupActivity
 *
 * TODO: ViewModelFactoryを実装して、依存性注入を行う
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var viewModel: SplashViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // ViewModelFactoryを使用してViewModelを生成
        val factory = ViewModelFactory(this)
        viewModel = ViewModelProvider(this, factory)[SplashViewModel::class.java]

        // 設定チェック処理を実行
        checkSettings()
    }

    /**
     * 設定の存在をチェックし、適切な画面へ遷移
     */
    private fun checkSettings() {
        lifecycleScope.launch {
            val hasSettings = viewModel.checkSettings()

            if (hasSettings) {
                navigateToMain()
            } else {
                navigateToSetup()
            }
        }
    }

    /**
     * メイン画面へ遷移
     */
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    /**
     * 設定画面へ遷移
     */
    private fun navigateToSetup() {
        val intent = Intent(this, SetupActivity::class.java)
        startActivity(intent)
        finish()
    }
}
