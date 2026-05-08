package com.example.model.request

import kotlinx.serialization.Serializable

@Serializable
data class PeticioLlista(
    val nomLlista: String? = null,
    val idsPropietaris: List<Int>? = null
)
