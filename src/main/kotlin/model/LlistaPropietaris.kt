package model

import kotlinx.serialization.Serializable

@Serializable
data class LlistaPropietaris(
    val idLlista : Int,
    val idUsuari: Int
)