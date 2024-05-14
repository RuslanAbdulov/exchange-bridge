package com.hgstrat.exchangebridge.service

import com.binance.connector.futures.client.impl.UMFuturesClientImpl
import com.hgstrat.exchangebridge.model.*
import com.hgstrat.exchangebridge.repository.AccountRepository
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val domainMapper: DomainMapper
) {

    private val accountClients = HashMap<String, UMFuturesClientImpl>()
    private lateinit var masterAccountClient: UMFuturesClientImpl

    @PostConstruct
    fun init() {
        reloadAccounts()
    }

    private fun reloadAccounts() {
        log.info("Reloading accounts")
        accountRepository.findByMasterIsTrue()
            .doOnNext {masterAccountClient = UMFuturesClientImpl(it.apiKey, it.secretKey)}
            .subscribe()
        accountRepository.findByActiveIsTrue()
            .doOnNext { accountClients[it.code] = UMFuturesClientImpl(it.apiKey, it.secretKey) }
            .subscribe()
    }

    fun createAccount(account: Account): Mono<out Any> {
        log.info("Adding account ${account.code}")
        val accountEntity = domainMapper.mapToEntity(account)
        return accountRepository.save(accountEntity)
            .doOnSuccess { reloadAccounts() }
    }

    fun exchangeClient(code: String): UMFuturesClientImpl? {
        return accountClients[code]
    }

    fun masterClient(): UMFuturesClientImpl {
        return masterAccountClient
    }

    fun activeAccounts(): Flux<String> {
        return accountRepository.findByActiveIsTrue()
            .map { it.code }
    }

    companion object {
        val log = LoggerFactory.getLogger(AccountService::class.java.name)
    }

}