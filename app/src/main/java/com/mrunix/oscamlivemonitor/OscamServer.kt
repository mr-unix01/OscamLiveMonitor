package com.mrunix.oscamlivemonitor

data class OscamServer(
    val nome: String,
    val host: String,
    val porta: String,
    val username: String,
    val password: String,
    val id: String = java.util.UUID.randomUUID().toString()
)
