package com.hgstrat.exchangebridge.model

enum class OrderState (val step: Int) {
    SIGNAL_RECEIVED(0),
    ORDER_PLACED(1),
    ORDER_FILLED(2),
    TP_SL_PLACED(3),
    COMPLETED(4),
    CANCELLED(5);

    companion object {
        fun getByName(name: String?) = when (name) {
            null -> SIGNAL_RECEIVED
            else -> valueOf(name.uppercase())// values().find { it.name.equals(name, true) }
        }
    }

    operator fun OrderState.compareTo(other: OrderState) = step.compareTo(other.step)

}