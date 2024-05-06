package com.hgstrat.exchangebridge.config

import com.hgstrat.exchangebridge.service.OrderService
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled


@EnableScheduling
@Configuration
class ScheduleConfiguration(val orderService: OrderService) {

    @Scheduled(fixedDelay = 60_000, initialDelay = 1_000)
    fun refreshOrders() {
        return orderService.refreshDBOrdersFromExchange()
    }

}