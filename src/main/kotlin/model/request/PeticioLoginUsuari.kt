package com.example.model.request

import kotlinx.serialization.Serializable

@Serializable
data class PeticioLoginUsuari(
    val username: String,
    val password: String,
)
