package com.hgstrat.exchangebridge.service

import com.binance.connector.futures.client.impl.UMFuturesClientImpl
import com.binance.connector.futures.client.impl.UMWebsocketClientImpl
import com.ninjasquad.springmockk.MockkBean
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest


@SpringBootTest
class OrderServiceTest {

    @MockkBean
    private lateinit var restClient: UMFuturesClientImpl

    @MockkBean
    private lateinit var wsClient: UMWebsocketClientImpl

    @Autowired
    private lateinit var orderService: OrderService

    private val allOrdersResponse = """
    [{
        "orderId": 6015477049,
        "symbol": "ONEUSDT",
        "status": "CANCELED",
        "clientOrderId": "web_st_4pxAn9aRO3Tp7qJ",
        "price": "0.04000",
        "avgPrice": "0.00000",
        "origQty": "125",
        "executedQty": "0",
        "cumQuote": "0",
        "timeInForce": "GTC",
        "type": "LIMIT",
        "reduceOnly": false,
        "closePosition": false,
        "side": "SELL",
        "positionSide": "BOTH",
        "stopPrice": "0",
        "workingType": "CONTRACT_PRICE",
        "priceMatch": "NONE",
        "selfTradePreventionMode": "NONE",
        "goodTillDate": 0,
        "priceProtect": false,
        "origType": "LIMIT",
        "time": 1714578091521,
        "updateTime": 1714828686580
    },
    {
        "orderId": 6021587628,
        "symbol": "ONEUSDT",
        "status": "FILLED",
        "clientOrderId": "web_OvuDNX3ZwpywxZJGWQu9",
        "price": "0.02000",
        "avgPrice": "0.02000",
        "origQty": "250",
        "executedQty": "250",
        "cumQuote": "5",
        "timeInForce": "GTC",
        "type": "LIMIT",
        "reduceOnly": false,
        "closePosition": false,
        "side": "BUY",
        "positionSide": "BOTH",
        "stopPrice": "0",
        "workingType": "CONTRACT_PRICE",
        "priceMatch": "NONE",
        "selfTradePreventionMode": "NONE",
        "goodTillDate": 0,
        "priceProtect": false,
        "origType": "LIMIT",
        "time": 1714828672152,
        "updateTime": 1714829491478
    }]
    """.trimIndent()

    @Test
    fun readAndWrapList() {

        val bOrders = orderService.readAndWrapList(allOrdersResponse)
        assertThat(bOrders).hasSize(2)
    }
}