package com.tinygc.asachiru.domain.repository

import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.Settings

/**
 * 設定を管理するリポジトリのインターフェース
 */
interface SettingsRepository {
    /**
     * 設定を取得
     * @return 設定
     */
    suspend fun getSettings(): Settings

    /**
     * 設定を保存
     * @param settings 設定
     * @return 保存結果（Result型）
     */
    suspend fun saveSettings(settings: Settings): Result<Unit>

    /**
     * 設定が存在するかチェック
     * @return 設定が存在する場合true
     */
    suspend fun hasSettings(): Boolean
}
