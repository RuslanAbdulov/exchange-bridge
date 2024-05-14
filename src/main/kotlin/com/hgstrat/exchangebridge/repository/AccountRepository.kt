package com.hgstrat.exchangebridge.repository

import com.hgstrat.exchangebridge.repository.entity.AccountEntity
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface AccountRepository: R2dbcRepository<AccountEntity, Int> {

    fun findByCode(code: String): Mono<AccountEntity>

    fun findByMasterIsTrue(): Mono<AccountEntity>

    fun findByActiveIsTrue(): Flux<AccountEntity>

}