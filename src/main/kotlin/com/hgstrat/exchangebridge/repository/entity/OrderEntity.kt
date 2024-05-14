package com.hgstrat.exchangebridge.repository.entity

import com.hgstrat.exchangebridge.model.OrderState
import jakarta.persistence.TemporalType
import org.springframework.data.annotation.Id
import org.springframework.data.jpa.repository.Temporal
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.Instant

@Table(name = "orders")
class OrderEntity (

    @Id
    var id: Int? = null,

    @Column
    val symbol: String,

    @Column
    var side: String,

    @Column
    val type: String,

    @Column
    val price: BigDecimal,

    @Column
    val quantity: BigDecimal,

    @Column
    val originOrderId: String?,

    @Column
    var exchangeOrderId: String? = null,

    @Column
    val timeFrame: String,

    @Column
    val stopLoss: BigDecimal? = null,

    @Column
    val takeProfit: BigDecimal? = null,

    @Column
    var state: OrderState? = OrderState.SIGNAL_RECEIVED,

    @Column("account_code")
    val account: String,

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    var lastUpdate: Instant? = null

)
