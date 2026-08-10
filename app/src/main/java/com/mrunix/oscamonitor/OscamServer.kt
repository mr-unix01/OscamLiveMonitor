package com.mrunix.oscamonitor

data class OscamServer(
    val nome: String,
    val host: String,
    val porta: String,
    val username: String,
    val password: String
)