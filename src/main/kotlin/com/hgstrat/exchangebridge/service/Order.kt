package com.hgstrat.exchangebridge.service

import java.math.BigDecimal
import java.time.Instant

data class Order(
    val symbol: String,//ONEUSDT
    val side: String, //SELL
    val type: String, //LIMIT
    val price: BigDecimal, //0.1
    val quantity: BigDecimal, //50
    val originOrderId: String?, //test1
    val goodTillDate: Instant?, //1713718068000
    val timeFrame: String?
)
