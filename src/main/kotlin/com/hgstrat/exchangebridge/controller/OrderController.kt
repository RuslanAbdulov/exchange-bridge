package com.hgstrat.exchangebridge.controller

import com.hgstrat.exchangebridge.model.Order
import com.hgstrat.exchangebridge.model.OrderResponse
import com.hgstrat.exchangebridge.service.OrderService
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController("/")
class OrderController (
    val orderService: OrderService
) {

    @GetMapping("/")
    fun index() = "Ok!"

    @PostMapping("/webhook/tv/{account}/")
    fun tvWebhook(@RequestBody order: Order,
                  @PathVariable("account") account: String) {
        orderService.process(order, account)
    }

    @PostMapping("/new-order/porxy/")
    fun newOrderProxy(@RequestBody parameters: LinkedHashMap<String, Any?>): String {
        return orderService.newOrderProxy(parameters)
    }

    @PostMapping("/new-order/ui/")
    fun newOrderViaUI(@RequestBody order: Order) {
//        uiService.placeOrder(order.price)
    }

    @PostMapping("/order/")
    fun saveOrder(@RequestBody order: Order): Mono<OrderResponse> {
        return orderService.saveOrder(order)
    }

    @GetMapping("/order/{symbol}")
    fun getSymbolOrders(@PathVariable symbol: String): Flux<OrderResponse> {
        return orderService.findBySymbol(symbol)
    }

    @GetMapping("/order/binance/")
    fun getExchangeOrders(): Flux<String> {
        return orderService.getAllFromExchangeAndUpdate()
            .map { it.toString() }
    }

}