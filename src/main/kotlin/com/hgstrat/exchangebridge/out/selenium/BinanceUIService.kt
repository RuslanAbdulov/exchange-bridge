package com.hgstrat.exchangebridge.out.selenium

import jakarta.annotation.PreDestroy
import org.openqa.selenium.WebDriver
import org.springframework.stereotype.Service
import java.math.BigDecimal

//@Service
class BinanceUIService(private val driver: WebDriver) {

    fun placeOrder(price: BigDecimal) {
        if (driver.currentUrl != "https://www.binance.com/en/futures/ONEUSDT") {
            driver.get("https://www.binance.com/en/futures/ONEUSDT");
        }
        FuturesPage(driver).setPrice(price.toString());
    }

    @PreDestroy
    fun tearDown() {
        driver.close()
    }

}