package com.hgstrat.exchangebridge.service

import com.binance.connector.futures.client.impl.UMFuturesClientImpl
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service


@Service
class OrderService(
    @Value("\${hgstrat.binance.api-key}") val apiKey: String,
    @Value("\${hgstrat.binance.secret-key}") val secretKey: String
) {

    var client: UMFuturesClientImpl = UMFuturesClientImpl(apiKey, secretKey)

    //TODO get api key for the account
    fun process(order: Order, account: String) {
        val parameters = LinkedHashMap<String, Any?>()

        parameters["symbol"] = order.symbol
        parameters["side"] = order.side//"SELL"
        parameters["type"] = order.type//"LIMIT"
        setGtd(parameters, order)

        parameters["quantity"] = order.quantity
        parameters["price"] = order.price
        parameters["newClientOrderId"] = order.originOrderId

        val result: String = client.account().newOrder(parameters)
    }

    //Also, goodTillDate can be calculated based on order.timeFrame
    private fun setGtd(parameters: LinkedHashMap<String, Any?>, order: Order) {
        if (order.goodTillDate != null) {
            parameters["timeInForce"] = "GTD"
            parameters["goodTillDate"] = order.goodTillDate.epochSecond
        } else {
            parameters["timeInForce"] = "GTC"
        }
    }

}