package com.hgstrat.exchangebridge.out.binance.futures

import com.hgstrat.exchangebridge.model.OrderState

class BOrderWrapper(private val bOrder: Map<String, Any?>) {


    fun getOriginOrderId(): String? {
        return bOrder["originOrderId"] as String?
    }

    fun getState(): OrderState? {
        return when (bOrder["status"]) {
            //is String -> OrderState.valueOf(bOrder["status"] as String)
            "FILLED" -> OrderState.ORDER_FILLED
            "CANCELED" -> OrderState.CANCELLED
            else -> null
        }

    }
}