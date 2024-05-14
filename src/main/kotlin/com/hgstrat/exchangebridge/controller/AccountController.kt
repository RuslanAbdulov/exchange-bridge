package com.hgstrat.exchangebridge.controller

import com.hgstrat.exchangebridge.model.Account
import com.hgstrat.exchangebridge.service.AccountService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/accounts")
class AccountController (
    val accountService: AccountService
) {
    companion object {
        val log = LoggerFactory.getLogger(AccountController::class.java.name)
    }

    @PostMapping("/")
    fun createAccount(@RequestBody account: Account): Mono<String> {
        return accountService.createAccount(account)
            .thenReturn("OK")
    }

}