package com.hgstrat.exchangebridge.service

enum class Side(val positionSide: String) {
    SELL("LONG") {
        override fun opposite() = BUY
    },
    BUY("SHORT") {
        override fun opposite() = SELL
    };

    abstract fun opposite(): Side

    companion object {
        fun getByName(name: String) = valueOf(name.uppercase())
    }

}