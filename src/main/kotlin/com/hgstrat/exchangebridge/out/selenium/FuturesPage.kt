package com.hgstrat.exchangebridge.out.selenium

import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.FindBy
import org.openqa.selenium.support.PageFactory

class FuturesPage(private val driver: WebDriver) {


    @FindBy(name = "limitPrice")
    private val limitPriceInput: WebElement? = null
    //<input data-bn-type="input" lang="en" id="limitPrice-732" name="limitPrice" class="order-form-input css-1wlt96c" step="0.00001" type="number" value="0.01718">

    //TODO div.name=orderForm / <button data-bn-type="button" type="submit" class=" css-1pzl47f">Buy/Long</button>

    init {
        PageFactory.initElements(driver, this)
    }

    fun setPrice(price: String) {
        //limitPriceInput?.click()
        limitPriceInput!!.clear()
        Actions(driver).doubleClick(limitPriceInput).perform()
        limitPriceInput.sendKeys(price)
    }
}