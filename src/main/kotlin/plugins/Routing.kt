package com.example.plugins

import com.example.plugins.routingV1.rutesDelSocket
import com.example.plugins.routingV1.rutesUsuaris
import io.ktor.server.application.Application
import io.ktor.server.routing.Route
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureRouting(){
    routing {
        rutesV1()
    }
}

private fun Routing.rutesV1() {
    route("/v1"){
        rutesUsuaris()
        rutesDelSocket()
    }

}

