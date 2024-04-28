package com.hgstrat.exchangebridge.controller

import com.hgstrat.exchangebridge.service.Order
import com.hgstrat.exchangebridge.service.OrderService
import org.springframework.web.bind.annotation.*

@RestController("/")
class OrderController (val orderService: OrderService) {

    @GetMapping("/")
    fun index() = "Ok!"

    @PostMapping("/webhook/tv/{account}/")
    fun tvWebhook(@RequestBody order: Order,
                  @PathVariable("account") account: String) {
        orderService.process(order, account)
    }

    @PostMapping("/new-order/")
    fun newOrderProxy(@RequestBody parameters: LinkedHashMap<String, Any?>): String {
        return orderService.newOrderProxy(parameters)
    }
}