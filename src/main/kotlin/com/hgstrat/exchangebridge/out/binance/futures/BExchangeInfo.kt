package com.hgstrat.exchangebridge.out.binance.futures

import java.math.BigDecimal

class BExchangeInfo(val symbols: List<Symbol>) {

    class Symbol(val symbol: String,
        val pair: String,
        val filters: List<Filter>) {

        class Filter(val filterType: String,
                     val tickSize: String?,
                     val stepSize: String?)

        fun tickSize(): BigDecimal {
            return filters.stream()
                .filter { it.filterType == "PRICE_FILTER" }
                .map { it.tickSize }
                .map { BigDecimal(it) }
                .findFirst()
                .orElseThrow()
        }

        fun stepSize(): BigDecimal {
            return filters.stream()
                .filter { it.filterType == "LOT_SIZE" }
                .map { it.stepSize }
                .map { BigDecimal(it) }
                .findFirst()
                .orElseThrow()
        }

    }

}