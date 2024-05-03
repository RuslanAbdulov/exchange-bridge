package com.hgstrat.exchangebridge.model

enum class OrderState {
    SIGNAL_RECEIVED,
    ORDER_PLACED,
    TP_SL_PLACED,
    ORDER_FILLED,
    COMPLETED,
    CANCELLED;

    companion object {
        fun getByName(name: String?) = when (name) {
            null -> SIGNAL_RECEIVED
            else -> valueOf(name.uppercase())// values().find { it.name.equals(name, true) }
        }
    }

}