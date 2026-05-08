package com.example

import com.example.plugins.bbdd.DatabaseFactory
import com.example.plugins.configureRouting
import com.example.plugins.configureSerialization
import com.example.plugins.security.configureSecurity
import com.example.plugins.security.configureSockets
import io.ktor.server.application.*
import repositoris.RepositoriUsuaris

fun main(args: Array<String>) {
    io.ktor.server.cio.EngineMain.main(args)
}

fun Application.module() {
    DatabaseFactory.init()
    configureSerialization()
    configureSecurity(RepositoriUsuaris)
    configureSockets()

    configureRouting()
}


