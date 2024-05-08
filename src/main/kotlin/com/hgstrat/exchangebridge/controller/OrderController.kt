package com.hgstrat.exchangebridge.controller

import com.hgstrat.exchangebridge.model.Order
import com.hgstrat.exchangebridge.service.OrderService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController("/")
class OrderController (
    val orderService: OrderService
) {
    companion object {
        val log = LoggerFactory.getLogger(OrderService::class.java.name)
    }

    @PostMapping("/webhook/tv/{account}/")
    fun tvWebhook(@RequestBody order: Order,
                  @PathVariable("account") account: String): Mono<String> {
        log.info("Account $account, order $order")
        return orderService.routeAndProcess(order, account)
    }

}