package com.hgstrat.exchangebridge.repository.entity

import jakarta.persistence.TemporalType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.repository.Temporal
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table(name = "accounts")
class AccountEntity (

    @Id
    var id: Int? = null,

    @Column
    val code: String,

    @Column("exchange")
    val exchange: String = "BINANCE",

    @Column
    val apiKey: String,

    @Column
    val secretKey: String,

    @Column
    val active: Boolean = true,

    @Column
    val master: Boolean = false,

    @Column
    @LastModifiedDate
    @Temporal(TemporalType.TIMESTAMP)
    var lastUpdate: Instant? = null

)