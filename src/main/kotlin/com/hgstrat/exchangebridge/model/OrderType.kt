package com.hgstrat.exchangebridge.model

enum class OrderType {
    LIMIT,
    CANCEL;

    companion object {
        fun getByName(name: String?) = entries.find { it.name.equals(name, true) }
    }

}