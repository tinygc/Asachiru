package com.tinygc.asachiru.domain.repository

import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.Weather

/**
 * 天気情報を取得するリポジトリのインターフェース
 */
interface WeatherRepository {
    /**
     * 指定された郵便番号の天気情報を取得
     * @param postalCode 郵便番号（7桁）
     * @return 天気情報（Result型）
     */
    suspend fun getWeather(postalCode: String): Result<Weather>
}
