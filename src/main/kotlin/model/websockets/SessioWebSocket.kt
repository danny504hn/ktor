package com.example.model.websockets

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.DefaultWebSocketSession

data class SessioWebSocket(
    val idUsuari : Int,
    val session: DefaultWebSocketServerSession
)
