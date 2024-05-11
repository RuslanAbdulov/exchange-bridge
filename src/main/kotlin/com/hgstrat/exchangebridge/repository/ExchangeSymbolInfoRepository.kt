package com.hgstrat.exchangebridge.repository

import com.hgstrat.exchangebridge.repository.entity.ExchangeSymbolInfoEntity
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Mono

interface ExchangeSymbolInfoRepository: R2dbcRepository<ExchangeSymbolInfoEntity, String> {

    fun findBySymbol(symbol: String): Mono<ExchangeSymbolInfoEntity>

}