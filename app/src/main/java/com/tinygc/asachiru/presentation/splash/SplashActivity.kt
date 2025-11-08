package com.tinygc.asachiru.presentation.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
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

        // TODO: ViewModelFactoryを使用してViewModelを生成
        // viewModel = ViewModelProvider(this, ViewModelFactory())[SplashViewModel::class.java]

        // 仮実装: すぐにチェック処理を実行
        // checkSettings()
    }

    /**
     * 設定の存在をチェックし、適切な画面へ遷移
     */
    private fun checkSettings() {
        lifecycleScope.launch {
            // TODO: ViewModelの実装後に有効化
            // val hasSettings = viewModel.checkSettings()
            val hasSettings = false // 仮の値

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
