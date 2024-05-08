package com.hgstrat.exchangebridge.service

import com.binance.connector.futures.client.impl.UMFuturesClientImpl
import com.binance.connector.futures.client.impl.UMWebsocketClientImpl
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.hgstrat.exchangebridge.model.*
import com.hgstrat.exchangebridge.out.binance.futures.BOrderWrapper
import com.hgstrat.exchangebridge.repository.OrderRepository
import com.hgstrat.exchangebridge.repository.entity.OrderEntity
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class OrderService(
    val restClient: UMFuturesClientImpl,
    val wsClient: UMWebsocketClientImpl,
    val objectMapper: ObjectMapper,
    val orderRepository: OrderRepository
) {

    companion object {
        val log = LoggerFactory.getLogger(OrderService::class.java.name)
    }

    //@PostConstruct
    fun listen() {
        //val listenKey = restClient.userData().createListenKey()
        //restClient.userData().extendListenKey()
        //TODO extract listenKey from {"listenKey":"YgUYs0sokBF1bB6GSJSaGzOixZ5B2rVf44xhQaJFYLL02qzKsOwWTDSSgz7ZE7p5"}
        // wsClient.combineStreams(listOf("ONEUSDT"))
        //wsClient.listenUserStream(listenKey, {} , {event -> log.info(event) }, {}, {});
    }


    fun refreshDBOrdersFromExchange() {
        getAllFromExchangeAndUpdate().subscribe()
    }

    fun getAllFromExchangeAndUpdate(): Flux<OrderEntity> {
        return orderRepository.findAllSymbols()
//            //filter by last update?
            .flatMapIterable { symbol ->
                val exchangeResponse = getFromExchange(symbol)
                readAndWrapList(exchangeResponse)
            }
            .mapNotNull { bOrder ->
                updateState(bOrder)
                    .filter { it.state == OrderState.ORDER_FILLED }
                    .flatMap { orderEntity ->
                        placeTakeProfit(mapToDomain(orderEntity))
                        placeStopLoss(mapToDomain(orderEntity))
                        orderEntity.state = OrderState.TP_SL_PLACED
                        orderRepository.save(orderEntity)
                    }
            }
            .flatMap { it }
    }

    fun updateState(bOrder: BOrderWrapper): Mono<OrderEntity> {
        if (bOrder.getState() == null) {
            log.warn("State not detected for Binance order ${bOrder.getOriginOrderId()}")
            return Mono.empty()
        }
        if (bOrder.getOriginOrderId() == null) {
            log.warn("No originOrderId to map Binance order")
            return Mono.empty()
        }
        return orderRepository.findByOriginOrderId(bOrder.getOriginOrderId()!!)
            .filter { order -> order.state!! < bOrder.getState() as OrderState }
            .flatMap { order ->
                order.state = bOrder.getState()
                orderRepository.save(order)
            }
    }

    fun readAndWrapList(response: String): List<BOrderWrapper> {
        val rawOrders: List<Map<String, Any?>> = objectMapper.readValue(response)
        return rawOrders.stream()
            .map { BOrderWrapper(it) }
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
    fun routeAndProcess(order: Order, account: String): Mono<String> {
        order.account = account
        return when (OrderType.getByName(order.type)) {
            OrderType.LIMIT -> placeLimitOrder(order)
            OrderType.CANCEL -> cancelOrder(symbol = order.symbol, originOrderId = order.originOrderId)
            else -> throw IllegalArgumentException("Unsupported order type ${order.type}")
        }

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

    private fun goodTillDate(order: Order): Instant {
        val minGoodTillDate = Instant.now().plusSeconds(601)
        if (order.goodTillDate != null && order.goodTillDate >= minGoodTillDate) {
            return order.goodTillDate
        }
        if (order.timeFrame == null) {
            return minGoodTillDate
        }
        val fromTimeFrame = Instant.now().plus(order.timeFrame.toLong(), ChronoUnit.MINUTES)
        return if (fromTimeFrame > minGoodTillDate) fromTimeFrame else minGoodTillDate
    }

//    fun stopLossStrategy(order: Order) {
//        val poResponse = placeLimitOrder(order)
//        val placedOrder: Map<String, String> = objectMapper.readValue(poResponse!!)
//
//        val takeProfitOrder: Map<String, String>
//        try {
//            val tpResponse = placeTakeProfit(order)
//            log.info(tpResponse)
//            takeProfitOrder = objectMapper.readValue(tpResponse)
//        } catch (e: Exception) {
//            cancelOrder(symbol = order.symbol, orderId = placedOrder["orderId"]!!.toLong())
//            return
//        }
//
//        try {
//            val slResponse = placeStopLoss(order)
//            log.info(slResponse)
//        } catch (e: Exception) {
//            cancelOrder(order.symbol, placedOrder["orderId"]!!.toLong())
//            cancelOrder(order.symbol, takeProfitOrder["orderId"]!!.toLong())
//            return
//        }
//    }

    fun placeLimitOrder(order: Order): Mono<String> {
        val parameters = LinkedHashMap<String, Any?>()

        parameters["side"] = Side.getByName(order.side).toString()
        parameters["positionSide"] = "BOTH"
        //parameters["positionSide"] = Side.getByName(order.side).positionSide
        parameters["symbol"] = trimSymbol(order.symbol)
        parameters["type"] = "LIMIT"
        parameters["goodTillDate"] = goodTillDate(order).toEpochMilli()
        parameters["timeInForce"] = "GTD" //"GTC"
        parameters["quantity"] = order.quantity
        parameters["price"] = order.price
        parameters["newClientOrderId"] = order.originOrderId
        //"securityType": "USDT_FUTURES",

        return saveOrder(order)
//            .doOnSuccess {restClient.account().newOrder(parameters) }
            .then(Mono.just(restClient.account().newOrder(parameters)))
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

    fun cancelOrder(symbol: String, orderId: Long? = null, originOrderId: String? = null): Mono<String> {
        val parameters = LinkedHashMap<String, Any?>()
        parameters["symbol"] = trimSymbol(symbol)
        parameters["orderId"] = orderId
        parameters["origClientOrderId"] = originOrderId

        return Mono.just(restClient.account().cancelOrder(parameters))
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
        return OrderEntity(
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
            account = order.account!!
        )
    }

    fun mapToDomain(order: OrderEntity): Order {
        return Order(
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
            lastUpdate = order.lastUpdate,
            account = order.account
        )
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
            lastUpdate = order.lastUpdate,
            account = order.account
        )
    }

    @PreDestroy
    fun destroy() {
        restClient.userData().closeListenKey()
        wsClient.closeAllConnections()
    }
}