package com.example.model.request

import kotlinx.serialization.Serializable

@Serializable
data class PeticioRegistreUsuari(
    val nomUsuari : String,
    val password : String,
    val alias : String?
)
