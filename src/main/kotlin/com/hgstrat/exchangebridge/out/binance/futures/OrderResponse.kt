package com.hgstrat.exchangebridge.out.binance.futures

import java.math.BigDecimal
import java.time.Instant

@Deprecated("use binance sdk")
data class OrderResponse(
    val orderId: Long,
    val symbol: String,
    val clientOrderId: String,
    val timeInForce: String,
    val goodTillDate: Instant,
    val updateTime: Instant,
    val type: String,
    val origType: String,
    val side: String,
    val positionSide: String,
    val price: BigDecimal,
    val origQty: BigDecimal)


//{
//    "orderId": 5990055068,
//    "symbol": "ONEUSDT",
//    "status": "NEW",
//    "clientOrderId": "test1",
//    "price": "0.10000",
//    "avgPrice": "0.00",
//    "origQty": "50",
//    "executedQty": "0",
//    "cumQty": "0",
//    "cumQuote": "0.00000",
//    "timeInForce": "GTD",
//    "type": "LIMIT",
//    "reduceOnly": false,
//    "closePosition": false,
//    "side": "SELL",
//    "positionSide": "BOTH",
//    "stopPrice": "0.00000",
//    "workingType": "CONTRACT_PRICE",
//    "priceProtect": false,
//    "origType": "LIMIT",
//    "priceMatch": "NONE",
//    "selfTradePreventionMode": "NONE",
//    "goodTillDate": 1713718068000,
//    "updateTime": 1713717419096
//}