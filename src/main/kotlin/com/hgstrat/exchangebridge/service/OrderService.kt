package com.hgstrat.exchangebridge.service

import com.binance.connector.futures.client.impl.UMFuturesClientImpl
import com.binance.connector.futures.client.impl.UMWebsocketClientImpl
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.hgstrat.exchangebridge.model.Order
import com.hgstrat.exchangebridge.model.OrderResponse
import com.hgstrat.exchangebridge.model.OrderState
import com.hgstrat.exchangebridge.model.Side
import com.hgstrat.exchangebridge.out.binance.futures.BOrderWrapper
import com.hgstrat.exchangebridge.repository.OrderRepository
import com.hgstrat.exchangebridge.repository.entity.OrderEntity
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal

@Service
class OrderService(
    val restClient: UMFuturesClientImpl,
    val wsClient: UMWebsocketClientImpl,
    val objectMapper: ObjectMapper,
    val orderRepository: OrderRepository,
) {

    companion object {
        val log = LoggerFactory.getLogger(OrderService::class.java.name)
    }

    //@PostConstruct
    fun listen() {
        restClient.account()
        val listenKey = restClient.userData().createListenKey()
        //restClient.userData().extendListenKey()
        //TODO extract listenKey from {"listenKey":"YgUYs0sokBF1bB6GSJSaGzOixZ5B2rVf44xhQaJFYLL02qzKsOwWTDSSgz7ZE7p5"}
        // wsClient.combineStreams(listOf("ONEUSDT"))
        wsClient.listenUserStream(listenKey, {} , {event -> log.info(event) }, {}, {});
    }


    fun refresh() {
//        orderRepository.todo() //find all symbols with open orders
//        restClient.account().allOrders();
    }

    fun getAllFromExchangeAndUpdate(): Flux<OrderEntity> {
        return orderRepository.findAllSymbols()
//            //filter by last update?
            .flatMapIterable { symbol ->
                val exchangeResponse = getFromExchange(symbol)
                readAndWrapList(exchangeResponse)
            }
            .mapNotNull { bOrder ->
                update(bOrder)
                    .flatMap { orderEntity ->
                        placeTakeProfit(mapToDomain(orderEntity))
                        placeStopLoss(mapToDomain(orderEntity))
                        orderEntity.state = OrderState.TP_SL_PLACED
                        orderRepository.save(orderEntity)
                    }
            }
            .flatMap {it}
            .log()
    }

//    orderRepository.findAllSymbols()
//                .mapNotNull { symbol ->
//                val exchangeResponse = getFromExchange(symbol)
//                val bOrder = readAndWrap(exchangeResponse)
//                update(bOrder)
//                    .flatMap { orderEntity ->
//                        placeTakeProfit(mapToDomain(orderEntity))
//                        placeStopLoss(mapToDomain(orderEntity))
//                        orderEntity.state = OrderState.TP_SL_PLACED
//                        orderRepository.save(orderEntity)
//                    }
//            }
//            .flatMap {it}
//            .log()

    fun update(bOrder: BOrderWrapper): Mono<OrderEntity> {
        if (bOrder.getState() == null || bOrder.getState()!! <= OrderState.ORDER_FILLED) {
            return Mono.empty()
        }
        if (bOrder.getOriginOrderId() == null) {
            return Mono.empty()
        }
        return orderRepository.findByOriginOrderId(bOrder.getOriginOrderId()!!)
            .filter { order -> order.state!! <= bOrder.getState() as OrderState }
            .flatMap { order ->
                order.state = bOrder.getState()
                orderRepository.save(order)
            }
    }

    fun readAndWrapList(response: String): List<BOrderWrapper> {
        val rawOrders: List<Map<String, Any?>> = objectMapper.readValue(response)
        return rawOrders.stream()
            .map {BOrderWrapper(it)}
            .toList()
    }

    fun getFromExchange(symbol: String): String {
        val parameters = LinkedHashMap<String, Any?>()
        parameters["symbol"] = trimSymbol(symbol)
        val allOrderResponse: String = restClient.account().allOrders(parameters)
        return allOrderResponse
    }

    fun getPosition(symbol: String): String {
        val parameters = LinkedHashMap<String, Any?>()
        return restClient.account().positionInformation(parameters)
    }


    //TODO get api key for the account
    fun process(order: Order, account: String) {
        val parameters = LinkedHashMap<String, Any?>()
        parameters["symbol"] = trimSymbol(order.symbol)
//        val tickerSymbol = client.market().tickerSymbol(parameters)

        stopLossStrategy(order)
    }

    private final inline fun <reified T> ObjectMapper.readValue(s: String): T =
        this.readValue(s, object : TypeReference<T>() {})
//    val typeRef: TypeReference<Map<String, String>> = object : TypeReference<Map<String, String>>() {}
//        val msg0:  Map<String,String> = objectMapper.readValue(result, typeRef)

    fun newOrderProxy(parameters: LinkedHashMap<String, Any?>): String {
        val result = restClient.account().newOrder(parameters)
        //val msg: Map<String,String> = objectMapper.readValue(result)
        return result
    }

    //Also, goodTillDate can be calculated based on order.timeFrame
    private fun setGtd(parameters: LinkedHashMap<String, Any?>, order: Order) {
        if (order.goodTillDate != null) {
            parameters["timeInForce"] = "GTD"
            parameters["goodTillDate"] = order.goodTillDate.epochSecond
        } else {
            parameters["timeInForce"] = "GTC"
        }
    }

    fun stopLossStrategy(order: Order) {
        val poResponse = placeOrder(order)
        val placedOrder: Map<String, String> = objectMapper.readValue(poResponse)

        val takeProfitOrder: Map<String, String>
        try {
            val tpResponse = placeTakeProfit(order)
            log.info(tpResponse)
            takeProfitOrder = objectMapper.readValue(tpResponse)
        } catch (e: Exception) {
            cancelOrder(order.symbol, placedOrder["orderId"]!!.toLong())
            return
        }

        try {
            val slResponse = placeStopLoss(order)
            log.info(slResponse)
        } catch (e: Exception) {
            cancelOrder(order.symbol, placedOrder["orderId"]!!.toLong())
            cancelOrder(order.symbol, takeProfitOrder["orderId"]!!.toLong())
            return
        }
    }

    //TODO goodTillDate or auto close by countdownTime, calc by timeframe and signal time
    // https://github.com/binance/binance-futures-connector-java/blob/main/src/test/java/examples/um_futures/account/AutoCancelOpen.java
    fun placeOrder(order: Order): String {
        val parameters = LinkedHashMap<String, Any?>()

        parameters["side"] = Side.getByName(order.side).toString()
        parameters["positionSide"] = "BOTH"
        //parameters["positionSide"] = Side.getByName(order.side).positionSide
        parameters["symbol"] = trimSymbol(order.symbol)
        parameters["type"] = "LIMIT"
        parameters["timeInForce"] = "GTC"
        parameters["quantity"] = order.quantity
        parameters["price"] = order.price
        parameters["newClientOrderId"] = order.originOrderId
        //"securityType": "USDT_FUTURES",
        //parameters["reduceOnly"] = false

        return restClient.account().newOrder(parameters)
    }

    fun placeTakeProfit(order: Order): String {
        return placeControlOrder(order, "TAKE_PROFIT", order.takeProfit!!)
    }

    fun placeStopLoss(order: Order): String {
        return placeControlOrder(order, "STOP", order.stopLoss!!)
    }

    fun placeControlOrder(order: Order, type: String, stopPrice: BigDecimal): String {
        val parameters = LinkedHashMap<String, Any?>()
        parameters["side"] = Side.getByName(order.side).opposite().toString()
        //parameters["positionSide"] = "BOTH" Side.getByName(order.side).positionSide
        parameters["symbol"] = trimSymbol(order.symbol)
        parameters["type"] = type //TAKE_PROFIT/TAKE_PROFIT_MARKET
        parameters["workingType"] = "MARK_PRICE" //CONTRACT_PRICE/MARK_PRICE
        parameters["priceProtect"] = true
        parameters["timeInForce"] = "GTE_GTC" //GTC
        parameters["quantity"] = order.quantity
        parameters["stopPrice"] = stopPrice
        parameters["price"] = order.price
        parameters["reduceOnly"] = true
        parameters["newClientOrderId"] =
            if (order.originOrderId != null)
                "${order.originOrderId}_${type.first()}"
            else
                null

        return restClient.account().newOrder(parameters)
    }

//    fun placeStopLoss(order: Order): String {
//        val parameters = LinkedHashMap<String, Any?>()
//        parameters["side"] = Side.getByName(order.side).opposite().toString()
//        parameters["symbol"] = trimSymbol(order.symbol)
//        parameters["type"] = "STOP"
//        parameters["workingType"] = "MARK_PRICE"
//        parameters["priceProtect"] = true
//        parameters["timeInForce"] = "GTE_GTC"
//        parameters["quantity"] = order.quantity
//        parameters["stopPrice"] = order.stopLoss
//        parameters["price"] = order.price
//        parameters["newClientOrderId"] = if (order.originOrderId != null) "${order.originOrderId}_sl" else null
//        parameters["reduceOnly"] = true
//
//        return client.account().newOrder(parameters)
//    }

    fun cancelOrder(symbol: String, orderId: Long): String {
        val parameters = LinkedHashMap<String, Any?>()
        parameters["symbol"] = trimSymbol(symbol)
        parameters["orderId"] = orderId

        return restClient.account().cancelOrder(parameters)
    }

    fun saveOrder(order: Order): Mono<OrderResponse> {
        val orderEntity = mapToEntity(order)
        return orderRepository.save(orderEntity)
            .map(this::mapToResponse)
    }

    fun findBySymbol(symbol: String): Flux<OrderResponse> {
        return orderRepository.findBySymbol(symbol)
            .map(this::mapToResponse)
    }

    fun trimSymbol(symbol: String): String {
        return if (symbol.endsWith(".P", ignoreCase = true))
            symbol.dropLast(2)
        else
            symbol
    }

    fun mapToEntity(order: Order): OrderEntity {
        return OrderEntity (
            symbol = order.symbol,
            side = order.side,
            type = order.type,
            price = order.price,
            quantity = order.quantity,
            originOrderId = order.originOrderId,
            timeFrame = order.timeFrame,
            stopLoss = order.stopLoss,
            takeProfit = order.takeProfit,
            state = order.state,
            exchangeOrderId = null)
    }

    fun mapToDomain(order: OrderEntity): Order {
        return Order (
            symbol = order.symbol,
            side = order.side,
            type = order.type,
            price = order.price,
            quantity = order.quantity,
            originOrderId = order.originOrderId,
            timeFrame = order.timeFrame,
            stopLoss = order.stopLoss,
            takeProfit = order.takeProfit,
            state = order.state,
            goodTillDate = null,
            lastUpdate = order.lastUpdate)
    }

    fun mapToResponse(order: OrderEntity): OrderResponse {
        return OrderResponse(
            id = order.id!!,
            symbol = order.symbol,
            state = order.state,
            side = order.side,
            type = order.type,
            price = order.price,
            quantity = order.quantity,
            originOrderId = order.originOrderId,
            exchangeOrderId = order.exchangeOrderId,
            timeFrame = order.timeFrame,
            stopLoss = order.stopLoss,
            takeProfit = order.takeProfit,
            lastUpdate = order.lastUpdate)
    }

    @PreDestroy
    fun destroy() {
        restClient.userData().closeListenKey()
        wsClient.closeAllConnections()
    }
}