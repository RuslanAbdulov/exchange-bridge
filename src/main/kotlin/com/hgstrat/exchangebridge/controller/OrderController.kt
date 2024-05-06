package com.hgstrat.exchangebridge.controller

import com.hgstrat.exchangebridge.model.Order
import com.hgstrat.exchangebridge.service.OrderService
import org.springframework.web.bind.annotation.*

@RestController("/")
class OrderController (
    val orderService: OrderService
) {

    @PostMapping("/webhook/tv/{account}/")
    fun tvWebhook(@RequestBody order: Order,
                  @PathVariable("account") account: String) {
        orderService.routeAndProcess(order, account)
    }

}