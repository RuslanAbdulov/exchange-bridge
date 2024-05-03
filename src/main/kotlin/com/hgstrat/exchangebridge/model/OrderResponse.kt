package com.hgstrat.exchangebridge.model

import java.math.BigDecimal
import java.time.Instant

data class OrderResponse(
    val id :Int,
    var symbol: String,//ONEUSDT
    var side: String?, //SELL
    var type: String?, //LIMIT
    var price: BigDecimal?, //0.1
    var quantity: BigDecimal?, //50
    var originOrderId: String?, //test1
    //var goodTillDate: Instant?, //1713718068000
    var timeFrame: String?,
    var stopLoss: BigDecimal?,
    var takeProfit: BigDecimal?,

    var state: OrderState?,
    var lastUpdate: Instant?

)
