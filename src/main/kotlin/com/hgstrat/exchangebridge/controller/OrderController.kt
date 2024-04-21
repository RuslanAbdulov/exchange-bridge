package com.hgstrat.exchangebridge.controller

import com.hgstrat.exchangebridge.service.Order
import com.hgstrat.exchangebridge.service.OrderService
import org.springframework.web.bind.annotation.*

@RestController("/")
class OrderController (val orderService: OrderService) {

    @GetMapping("/test")
    fun index(@RequestParam("name", required = false) name: String) = "Hello, $name!"

    @PostMapping("/webhook/tv/{account}/")
    fun tvWebhook(@RequestBody order: Order,
                  @PathVariable("account") account: String) {
        orderService.process(order, account)
    }
}