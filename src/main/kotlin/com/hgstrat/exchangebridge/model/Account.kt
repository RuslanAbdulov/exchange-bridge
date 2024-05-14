package com.hgstrat.exchangebridge.model

data class Account(
    val code: String,
    val apiKey: String,
    val secretKey: String,
    val active: Boolean = true,
    val master: Boolean = false,
)
