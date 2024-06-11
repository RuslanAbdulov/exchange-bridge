package com.hgstrat.exchangebridge.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.hgstrat.exchangebridge.model.*
import com.hgstrat.exchangebridge.out.binance.futures.BExchangeInfo
import com.hgstrat.exchangebridge.out.binance.futures.BOrderWrapper
import com.hgstrat.exchangebridge.repository.ExchangeSymbolInfoRepository
import com.hgstrat.exchangebridge.repository.OrderRepository
import com.hgstrat.exchangebridge.repository.entity.ExchangeSymbolInfoEntity
import com.hgstrat.exchangebridge.repository.entity.OrderEntity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.log10

@Service
class OrderService(
    val accountService: AccountService,
    val objectMapper: ObjectMapper,
    val orderRepository: OrderRepository,
    val exchangeRepository: ExchangeSymbolInfoRepository,
    val domainMapper: DomainMapper
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
        return accountService.activeAccounts()
            .flatMap { getAllFromExchangeAndUpdate(it) }
    }
    fun getAllFromExchangeAndUpdate(account: String): Flux<OrderEntity> {
        return orderRepository.findAllSymbols(account)
//            //filter by last update?
            .flatMapIterable { symbol ->
                val exchangeResponse = getFromExchange(symbol, account)
                readAndWrapOrderList(exchangeResponse)
            }
            .mapNotNull { bOrder ->
                updateState(bOrder)
                    .filter { it.state == OrderState.ORDER_FILLED }
                    .flatMap { orderEntity ->
                        val domainOrder = domainMapper.mapToDomain(orderEntity)
                        placeTakeProfit(domainOrder, account)
                        placeStopLoss(domainOrder, account)
                        placeTrailingStop(domainOrder, account)
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

    fun readAndWrapOrderList(response: String): List<BOrderWrapper> {
        val rawOrders: List<Map<String, Any?>> = objectMapper.readValue(response)
        return rawOrders.stream()
            .map { BOrderWrapper(it) }
            .toList()
    }

    fun readExchangeInfo(response: String): BExchangeInfo {
        val exchangeInfo: BExchangeInfo = objectMapper.readValue(response)
        return exchangeInfo
    }

    fun getFromExchange(symbol: String, account: String): String {
        val parameters = LinkedHashMap<String, Any?>()
        parameters["symbol"] = trimSymbol(symbol)
        val allOrderResponse: String = accountService.exchangeClient(account)!!.account().allOrders(parameters)
        return allOrderResponse
    }

    fun routeAndProcess(order: Order, account: String): Mono<String> {
        order.account = account
        return when (OrderType.getByName(order.type)) {
            OrderType.LIMIT -> placeLimitOrder(order, account)
            OrderType.CANCEL -> cancelOrder(
                symbol = order.symbol,
                originOrderId = order.originOrderId,
                account = account)
            else -> throw IllegalArgumentException("Unsupported order type ${order.type}")
        }

    }

    private final inline fun <reified T> ObjectMapper.readValue(s: String): T =
        this.readValue(s, object : TypeReference<T>() {})
//    val typeRef: TypeReference<Map<String, String>> = object : TypeReference<Map<String, String>>() {}
//        val msg0:  Map<String,String> = objectMapper.readValue(result, typeRef)

    fun newOrderProxy(parameters: LinkedHashMap<String, Any?>): String {
        val result = accountService.masterClient().account().newOrder(parameters)
        //val msg: Map<String,String> = objectMapper.readValue(result)
        return result
    }

    private fun goodTillDate(order: Order): Instant {
        val minGoodTillDate = Instant.now().plusSeconds(605)
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
    fun placeLimitOrder(order: Order, account: String): Mono<String> {
        val symbol = trimSymbol(order.symbol)

        return Mono.zip(Mono.just(order), exchangeRepository.findBySymbol(symbol))
            .map { tuple ->
                    val priceScale = -log10(tuple.t2.tickSize.toDouble()).toInt()
                    val quantityScale = -log10(tuple.t2.stepSize.toDouble()).toInt()

                    tuple.t1.copy(
                        symbol = symbol,
                        price = tuple.t1.price.setScale(priceScale, RoundingMode.HALF_UP),
                        takeProfit = tuple.t1.takeProfit?.setScale(priceScale, RoundingMode.HALF_UP),
                        stopLoss = tuple.t1.stopLoss?.setScale(priceScale, RoundingMode.HALF_UP),
                        quantity = tuple.t1.quantity.setScale(quantityScale, RoundingMode.HALF_UP),
                    )
            }
            .doOnNext{ saveOrder(it).subscribeOn(Schedulers.parallel()).subscribe() }
            .map { accountService.exchangeClient(account)!!.account().newOrder(toParameters(it)) }
    }

    fun toParameters(order: Order): LinkedHashMap<String, Any?> {
        val parameters = LinkedHashMap<String, Any?>()
        parameters["side"] = Side.getByName(order.side).toString()
        parameters["positionSide"] = "BOTH"
        //parameters["positionSide"] = Side.getByName(order.side).positionSide
        parameters["symbol"] = order.symbol
        parameters["type"] = "LIMIT"
        parameters["goodTillDate"] = goodTillDate(order).toEpochMilli()
        parameters["timeInForce"] = "GTD"
        parameters["quantity"] = order.quantity
        parameters["price"] = order.price
        parameters["newClientOrderId"] = order.originOrderId

        return parameters
    }

    fun placeTakeProfit(order: Order, account: String): String {
        return placeControlOrder(order, "TAKE_PROFIT", order.takeProfit!!, account)
    }

    fun placeStopLoss(order: Order, account: String): String {
        return placeControlOrder(order, "STOP", order.stopLoss!!, account)
    }

    fun placeTrailingStop(order: Order, account: String): String {
        if (order.trailingStopCallbackRate == null) {
            return ""
        }
        val parameters = LinkedHashMap<String, Any?>()
        parameters["side"] = Side.getByName(order.side).opposite().toString()
        parameters["symbol"] = trimSymbol(order.symbol)
        parameters["type"] = "TRAILING_STOP_MARKET"
        parameters["workingType"] = "MARK_PRICE" //CONTRACT_PRICE/MARK_PRICE
        parameters["timeInForce"] = "GTE_GTC"
        parameters["quantity"] = order.quantity
        parameters["callbackRate"] = order.trailingStopCallbackRate
        parameters["activationPrice"] = order.trailingStopActivationPrice
        parameters["reduceOnly"] = true
        parameters["newClientOrderId"] =
            if (order.originOrderId != null)
                "${order.originOrderId}_trail"
            else
                null

        return accountService.exchangeClient(account)!!.account().newOrder(parameters)
    }

    fun placeControlOrder(order: Order, type: String, stopPrice: BigDecimal, account: String): String {
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
        parameters["price"] = stopPrice
        parameters["reduceOnly"] = true
        parameters["newClientOrderId"] =
            if (order.originOrderId != null)
                "${order.originOrderId}_${type.first()}"
            else
                null

        return accountService.exchangeClient(account)!!.account().newOrder(parameters)
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

    fun cancelOrder(symbol: String,
                    orderId: Long? = null,
                    originOrderId: String? = null,
                    account: String): Mono<String> {
        val parameters = LinkedHashMap<String, Any?>()
        parameters["symbol"] = trimSymbol(symbol)
        parameters["orderId"] = orderId
        parameters["origClientOrderId"] = originOrderId

        return Mono.just(accountService.exchangeClient(account)!!.account().cancelOrder(parameters))
    }

    fun saveOrder(order: Order): Mono<OrderResponse> {
        val orderEntity = domainMapper.mapToEntity(order)
        return orderRepository.save(orderEntity)
            .map(domainMapper::mapToResponse)
    }

    fun updateExchangeInfo(): Mono<Void> {
        return Mono.just(accountService.masterClient().market().exchangeInfo())
            .map { readExchangeInfo(it) }
            .flatMapMany { saveExchangeInfo(it) }
            .then()
    }

    fun saveExchangeInfo(exchangeInfo: BExchangeInfo): Flux<ExchangeSymbolInfoEntity> {
        return Flux.fromStream(
            exchangeInfo.symbols.stream()
                .map { domainMapper.mapToEntity(it) }
                .map { exchangeRepository.save(it) }
        )
            .flatMap { it }
    }

    fun findBySymbol(symbol: String): Flux<OrderResponse> {
        return orderRepository.findBySymbol(symbol)
            .map(domainMapper::mapToResponse)
    }

    fun trimSymbol(symbol: String): String {
        return if (symbol.endsWith(".P", ignoreCase = true))
            symbol.dropLast(2)
        else
            symbol
    }

}