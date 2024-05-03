package com.hgstrat.exchangebridge.repository

import com.hgstrat.exchangebridge.repository.entity.OrderEntity
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux

interface OrderRepository: R2dbcRepository<OrderEntity, Int> {

    fun findBySymbol(symbol: String): Flux<OrderEntity>

}