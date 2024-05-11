package com.hgstrat.exchangebridge.repository.entity

import jakarta.persistence.TemporalType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Version
import org.springframework.data.jpa.repository.Temporal
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.Instant

@Table(name = "exchange_symbol_info")
class ExchangeSymbolInfoEntity (

    @Id
    @Column
    val symbol: String,

    @Column("exchange")
    val exchange: String = "BINANCE",

    @Column
    val pair: String,

    @Column("tick_size")
    val tickSize: BigDecimal,

    @Column("step_size")
    val stepSize: BigDecimal,

    @Column
    @LastModifiedDate
    @Temporal(TemporalType.TIMESTAMP)
    var lastUpdate: Instant? = null,

    @Column
    @Version
    val version: Int = 0

)