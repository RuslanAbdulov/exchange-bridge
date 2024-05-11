package com.hgstrat.exchangebridge.config

import io.r2dbc.spi.ConnectionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator
import org.springframework.scheduling.annotation.EnableScheduling


@EnableScheduling
@Configuration
class RepositoryConfiguration {

    @Bean
    fun initializer(connectionFactory: ConnectionFactory): ConnectionFactoryInitializer {
        val initializer = ConnectionFactoryInitializer()
        initializer.setConnectionFactory(connectionFactory)

        val populator = CompositeDatabasePopulator()
        populator.addPopulators(ResourceDatabasePopulator(ClassPathResource("db/schema.sql")))
        initializer.setDatabasePopulator(populator)

        return initializer
    }

//    @Bean(initMethod = "start", destroyMethod = "stop")
//    @Throws(SQLException::class)
//    fun h2Server(): Server {
//        return Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9021")
//    }

}