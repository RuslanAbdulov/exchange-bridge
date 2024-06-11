package com.hgstrat.exchangebridge.service

import com.hgstrat.exchangebridge.model.*
import com.hgstrat.exchangebridge.out.binance.futures.BExchangeInfo
import com.hgstrat.exchangebridge.repository.entity.AccountEntity
import com.hgstrat.exchangebridge.repository.entity.ExchangeSymbolInfoEntity
import com.hgstrat.exchangebridge.repository.entity.OrderEntity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DomainMapper {

    companion object {
        val log = LoggerFactory.getLogger(DomainMapper::class.java.name)
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
            price = order.price!!,
            quantity = order.quantity!!,
            originOrderId = order.originOrderId,
            timeFrame = order.timeFrame,
            stopLoss = order.stopLoss,
            takeProfit = order.takeProfit,
            trailingStopCallbackRate = order.trailingStopCallbackRate,
            trailingStopActivationPrice = order.trailingStopActivationPrice,
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
            trailingStopCallbackRate = order.trailingStopCallbackRate,
            trailingStopActivationPrice = order.trailingStopActivationPrice,
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
            trailingStopCallbackRate = order.trailingStopCallbackRate,
            trailingStopActivationPrice = order.trailingStopActivationPrice,
            lastUpdate = order.lastUpdate,
            account = order.account
        )
    }

    fun mapToEntity(exchangeSymbol: BExchangeInfo.Symbol): ExchangeSymbolInfoEntity {
        return ExchangeSymbolInfoEntity(
            symbol = exchangeSymbol.symbol,
            pair = exchangeSymbol.pair,
            tickSize = exchangeSymbol.tickSize(),
            stepSize = exchangeSymbol.stepSize()
        )
    }

    fun mapToEntity(account: Account): AccountEntity {
        return AccountEntity(
            code = account.code,
            name = account.name,
            apiKey = account.apiKey,
            secretKey = account.secretKey,
            active = account.active,
            master = account.master)
    }

}