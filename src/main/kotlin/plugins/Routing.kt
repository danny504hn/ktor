package com.example.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.html.respondHtml
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.title
import kotlinx.html.head
import kotlinx.html.p

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        route("html"){
            get{
                call.respondHtml(status = HttpStatusCode.OK){
                    head{
                        title { "Montilivi Llista Compra" }
                    }
                    body{
                        h1 { +"Benvingut a la llista de la compra de Montilivi" }
                        p { +"Això és un microservei implementat en Ktor"}
                        div{
                            a(href="click") {
                                p{+ "Fes click"}
                            }
                        }
                    }
                }
            }

            get("/click"){
                call.respondHtml(status = HttpStatusCode.OK){
                    head{
                        title { "Has fet click" }
                    }
                    body{
                        h1 { +"Has fet click al link" }
                        p { +"Gràcies per la teva visita"}
                        div{
                            a(href="html"){
                                +"Torna a la pàgina principal"
                            }
                        }
                    }
                }
            }
        }
    }
}
