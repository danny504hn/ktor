package com.example.model.request

import kotlinx.serialization.Serializable

@Serializable
data class PeticioProducte(
    val nomProducte: String? = null,
    val idCategoria: Int? = null
)
