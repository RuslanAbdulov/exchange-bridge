package com.hgstrat.exchangebridge.repository

import com.hgstrat.exchangebridge.repository.entity.OrderEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface OrderRepository: R2dbcRepository<OrderEntity, Int> {

    fun findBySymbol(symbol: String): Flux<OrderEntity>

    fun findByOriginOrderId(originOrderId: String): Mono<OrderEntity>

    @Query("select distinct symbol from orders where account_code = :account")
    fun findAllSymbols(account: String): Flux<String>

}