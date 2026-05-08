package com.example.plugins.routingV1

import com.example.model.websockets.SessioWebSocket
import com.example.utils.GestorDeConnexions
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText

fun Route.rutesDelSocket(){
    authenticate("auth-jwt") { webSocket("/websocket") {

        val principal = call.principal<JWTPrincipal>()!!

        val idUsuari =  principal?.payload?.getClaim("idUsuari")?.asInt()
            ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY,"Usuari no existeix"))
        val sessioNova = SessioWebSocket(idUsuari = idUsuari, session = this)
        GestorDeConnexions.afegeix(sessioNova)


        try{
            for(frame in incoming){
                if(frame is Frame.Text){
                    val text = frame.readText()
                    println(text)
                }
            }
        }finally {
            GestorDeConnexions.elimina(1)
        }
    } }

}