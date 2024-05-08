package com.hgstrat.exchangebridge.model

import java.math.BigDecimal
import java.time.Instant

data class Order(
    val symbol: String,//ONEUSDT
    val side: String, //SELL
    val type: String, //LIMIT
    val price: BigDecimal, //0.1
    val quantity: BigDecimal, //50
    val originOrderId: String? = null, //test1
    val goodTillDate: Instant? = null, //1713718068000
    val timeFrame: String,
    val stopLoss: BigDecimal? = null,
    val takeProfit: BigDecimal? = null,
    //could calc quantity from price and sum
    val sum: BigDecimal? = null,

    val state: OrderState? = OrderState.SIGNAL_RECEIVED,
    val lastUpdate: Instant? = null,
    var account: String?

)
