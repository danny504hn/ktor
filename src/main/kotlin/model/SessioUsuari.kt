package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class SessioUsuari(
    val idUsuari: String,
    val username : String
)