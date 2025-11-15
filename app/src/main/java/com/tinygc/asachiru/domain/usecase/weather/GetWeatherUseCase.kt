package com.tinygc.asachiru.domain.usecase.weather

import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.Weather
import com.tinygc.asachiru.domain.repository.SettingsRepository
import com.tinygc.asachiru.domain.repository.WeatherRepository

/**
 * 天気情報を取得するユースケース
 */
class GetWeatherUseCase(
    private val weatherRepository: WeatherRepository,
    private val settingsRepository: SettingsRepository
) {
    /**
     * 設定された郵便番号の天気情報を取得
     * @return 天気情報（Result型）
     */
    suspend operator fun invoke(): Result<Weather> {
        return try {
            val settings = settingsRepository.getSettings()
            weatherRepository.getWeather(settings.postalCode)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
