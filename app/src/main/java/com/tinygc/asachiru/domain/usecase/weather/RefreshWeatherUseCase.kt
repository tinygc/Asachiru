package com.tinygc.asachiru.domain.usecase.weather

import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.Weather

/**
 * 天気情報を強制的に再取得するユースケース
 */
class RefreshWeatherUseCase(
    private val getWeatherUseCase: GetWeatherUseCase
) {
    /**
     * 天気情報を強制的に再取得
     * @return 天気情報（Result型）
     */
    suspend operator fun invoke(): Result<Weather> {
        return getWeatherUseCase()
    }
}
