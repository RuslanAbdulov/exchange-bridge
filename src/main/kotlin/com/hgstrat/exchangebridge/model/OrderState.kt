package com.hgstrat.exchangebridge.model

enum class OrderState (private val step: Int) {
    SIGNAL_RECEIVED(0),
    ORDER_PLACED(1),
    ORDER_FILLED(2),
    TP_SL_PLACED(3),
    COMPLETED(4),
    CANCELLED(5),
    REJECTED(6);

    companion object {

        private val CANCEL_STATES = setOf(CANCELLED, REJECTED)

        fun isCancelState(state: OrderState?): Boolean = CANCEL_STATES.contains(state)

        fun getByName(name: String?) = when (name) {
            null -> SIGNAL_RECEIVED
            else -> valueOf(name.uppercase())// values().find { it.name.equals(name, true) }
        }
    }

    operator fun OrderState.compareTo(other: OrderState) = step.compareTo(other.step)

    fun isCancelState(): Boolean = Companion.isCancelState(this)

}