package com.hgstrat.exchangebridge.config

import com.binance.connector.futures.client.impl.UMFuturesClientImpl
import com.binance.connector.futures.client.impl.UMWebsocketClientImpl
import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BinanceConfiguration(
    @Value("\${hgstrat.binance.api-key}") val apiKey: String,
    @Value("\${hgstrat.binance.secret-key}") val secretKey: String) {

    @Bean
    fun binanceRestClient(): UMFuturesClientImpl {
        return UMFuturesClientImpl(apiKey, secretKey)
    }

    @Bean
    fun binanceWSClient(): UMWebsocketClientImpl {
        return UMWebsocketClientImpl()
    }

}