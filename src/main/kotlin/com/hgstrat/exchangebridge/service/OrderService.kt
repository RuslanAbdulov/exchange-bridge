package com.hgstrat.exchangebridge.service

import com.binance.connector.futures.client.impl.UMFuturesClientImpl
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class OrderService(
    @Value("\${hgstrat.binance.api-key}") val apiKey: String,
    @Value("\${hgstrat.binance.secret-key}") val secretKey: String,
    val objectMapper: ObjectMapper
) {
    companion object {
        val LOG = LoggerFactory.getLogger(OrderService::class.java.name)
    }

    var client: UMFuturesClientImpl = UMFuturesClientImpl(apiKey, secretKey)

    //TODO get api key for the account
    fun process(order: Order, account: String) {
        val parameters = LinkedHashMap<String, Any?>()
        parameters["symbol"] = trimSymbol(order.symbol)
//        val tickerSymbol = client.market().tickerSymbol(parameters)

        stopLossStrategy(order)
    }

    private final inline fun <reified T> ObjectMapper.readValue(s: String): T =
        this.readValue(s, object : TypeReference<T>() {})
    //then val msg: Map<String,String> = objectMapper.readValue(result)
//    val typeRef: TypeReference<Map<String, String>> = object : TypeReference<Map<String, String>>() {}
//        val msg0:  Map<String,String> = objectMapper.readValue(result, typeRef)

    fun newOrderProxy(parameters: LinkedHashMap<String, Any?>): String {
        val result = client.account().newOrder(parameters)
        //val msg: Map<String,String> = objectMapper.readValue(result)
        return result
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

    fun stopLossStrategy(order: Order) {
        val poResponse = placeOrder(order)
        val placedOrder: Map<String, String> = objectMapper.readValue(poResponse)

        val takeProfitOrder: Map<String, String>
        try {
            val tpResponse = placeTakeProfit(order)
            LOG.info(tpResponse)
            takeProfitOrder = objectMapper.readValue(tpResponse)
        } catch (e: Exception) {
            cancelOrder(order.symbol, placedOrder["orderId"]!!.toLong())
            return
        }

        try {
            val slResponse = placeStopLoss(order)
            LOG.info(slResponse)
        } catch (e: Exception) {
            cancelOrder(order.symbol, placedOrder["orderId"]!!.toLong())
            cancelOrder(order.symbol, takeProfitOrder["orderId"]!!.toLong())
            return
        }
    }

    //TODO goodTillDate
    fun placeOrder(order: Order): String {
        val parameters = LinkedHashMap<String, Any?>()

        parameters["side"] = Side.getByName(order.side).toString()
        parameters["positionSide"] = "BOTH"
        //parameters["positionSide"] = Side.getByName(order.side).positionSide
        parameters["symbol"] = trimSymbol(order.symbol)
        parameters["type"] = "LIMIT"
        parameters["timeInForce"] = "GTC"
        parameters["quantity"] = order.quantity
        parameters["price"] = order.price
        parameters["newClientOrderId"] = order.originOrderId
        //"securityType": "USDT_FUTURES",
        //parameters["reduceOnly"] = false

        return client.account().newOrder(parameters)
    }

    fun placeTakeProfit(order: Order): String {
        return placeControlOrder(order, "TAKE_PROFIT", order.takeProfit!!)
    }

    fun placeStopLoss(order: Order): String {
        return placeControlOrder(order, "STOP", order.stopLoss!!)
    }

    fun placeControlOrder(order: Order, type: String, stopPrice: BigDecimal): String {
        val parameters = LinkedHashMap<String, Any?>()
        parameters["side"] = Side.getByName(order.side).opposite().toString()
        //parameters["positionSide"] = "BOTH" Side.getByName(order.side).positionSide
        parameters["symbol"] = trimSymbol(order.symbol)
        parameters["type"] = type //TAKE_PROFIT/TAKE_PROFIT_MARKET
        parameters["workingType"] = "MARK_PRICE" //CONTRACT_PRICE/MARK_PRICE
        parameters["priceProtect"] = true
        parameters["timeInForce"] = "GTE_GTC" //GTC
        parameters["quantity"] = order.quantity
        parameters["stopPrice"] = stopPrice
        parameters["price"] = order.price
        parameters["reduceOnly"] = true
        parameters["newClientOrderId"] =
            if (order.originOrderId != null)
                "${order.originOrderId}_${type.first()}"
            else
                null

        return client.account().newOrder(parameters)
    }

//    fun placeStopLoss(order: Order): String {
//        val parameters = LinkedHashMap<String, Any?>()
//        parameters["side"] = Side.getByName(order.side).opposite().toString()
//        parameters["symbol"] = trimSymbol(order.symbol)
//        parameters["type"] = "STOP"
//        parameters["workingType"] = "MARK_PRICE"
//        parameters["priceProtect"] = true
//        parameters["timeInForce"] = "GTE_GTC"
//        parameters["quantity"] = order.quantity
//        parameters["stopPrice"] = order.stopLoss
//        parameters["price"] = order.price
//        parameters["newClientOrderId"] = if (order.originOrderId != null) "${order.originOrderId}_sl" else null
//        parameters["reduceOnly"] = true
//
//        return client.account().newOrder(parameters)
//    }

    fun cancelOrder(symbol: String, orderId: Long): String {
        val parameters = LinkedHashMap<String, Any?>()
        parameters["symbol"] = trimSymbol(symbol)
        parameters["orderId"] = orderId

        return client.account().cancelOrder(parameters)
    }


    fun trimSymbol(symbol: String): String {
        return if (symbol.endsWith(".P", ignoreCase = true))
            symbol.dropLast(2)
        else
            symbol
    }



}