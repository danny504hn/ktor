package com.example.utils

import com.example.model.SessioUsuari
import com.example.model.websockets.SessioWebSocket
import com.example.plugins.routingV1.EsdevenimentLlista
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.sendSerialized
import java.net.http.WebSocket
import java.util.concurrent.ConcurrentHashMap

object GestorDeConnexions {
    private val sessions = ConcurrentHashMap<Int, SessioWebSocket>()

    fun afegeix(sessio: SessioWebSocket){
        sessions[sessio.idUsuari] = sessio
    }
    fun elimina(idUsuari:Int){
        sessions.remove(idUsuari)
    }
    fun obten(idUsuari: Int) = sessions[idUsuari]

    fun obtenTotes(): List<SessioWebSocket> = sessions.values.toList()

    suspend fun enviaAUsuarisConcrects(idsUsuaris: List<Int>,
                                       esdevenimet: EsdevenimentLlista){
        idsUsuaris.forEach { id ->
            sessions[id]?.let { sessioUsuari ->
                try{
                    sessioUsuari.session.sendSerialized(esdevenimet)
                }catch (e: Exception){
                    println("Error enviant missatge a $id : ${e.message}")
                }
            }
        }
    }
}