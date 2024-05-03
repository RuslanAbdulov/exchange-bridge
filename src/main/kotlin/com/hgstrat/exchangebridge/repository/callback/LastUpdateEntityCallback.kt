package com.hgstrat.exchangebridge.repository.callback

import com.hgstrat.exchangebridge.repository.entity.OrderEntity
import org.reactivestreams.Publisher
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback
import org.springframework.data.relational.core.sql.SqlIdentifier
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Instant

@Component
class LastUpdateEntityCallback : BeforeConvertCallback<OrderEntity> {

    override fun onBeforeConvert(entity: OrderEntity, table: SqlIdentifier): Publisher<OrderEntity> {
        entity.lastUpdate = Instant.now()
        return Mono.just(entity);
    }

}