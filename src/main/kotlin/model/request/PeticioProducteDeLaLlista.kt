package com.example.model.request

import kotlinx.serialization.Serializable

@Serializable
data class PeticioProducteDeLaLlista(
    val idProducte: Int? = null,
    val nomProducte: String? = null,
    val quantitat: Int? = null,
    val unitat: String? = null,
    val estaComprat: Boolean? = null,
    val quiHaComprat: Int? = null
)