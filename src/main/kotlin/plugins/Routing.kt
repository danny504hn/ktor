package com.example.plugins

import com.example.plugins.routingV1.rutesCategories
import com.example.plugins.routingV1.rutesDelSocket
import com.example.plugins.routingV1.rutesLlistes
import com.example.plugins.routingV1.rutesProductes
import com.example.plugins.routingV1.rutesUsuaris
import io.ktor.http.ContentType
import io.ktor.openapi.OpenApiInfo
import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.Route
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.routingRoot

fun Application.configureRouting(){
    routing {
        rutesV1()
        swaggerUI("/swagger") {
            info = OpenApiInfo("API LListaCompra", "1.0")
            source = OpenApiDocSource.Routing(ContentType.Application.Json) {
                routingRoot.descendants()
            }
        }
    }
}

private fun Routing.rutesV1() {
    route("/v1"){
        rutesUsuaris()
        rutesDelSocket()
        rutesCategories()
        rutesProductes()
        rutesLlistes()
    }

}

