package com.hgstrat.exchangebridge.config

import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

//@Configuration
class SeleniumConfiguration {

    @Bean
    fun chromeDriver(chromeOptions: ChromeOptions?): WebDriver {
        WebDriverManager.chromedriver().setup()
        return ChromeDriver(chromeOptions)
    }

    @Bean
    fun chromeOptions(): ChromeOptions {
        val chromeOptions = ChromeOptions()
        //chromeOptions.addArguments("--remote-debugging-port=9222")

        // run in headless mode
        // chromeOptions.addArguments("--no-sandbox");
        // chromeOptions.addArguments("--headless");
        // chromeOptions.addArguments("disable-gpu");
        return chromeOptions
    }
}