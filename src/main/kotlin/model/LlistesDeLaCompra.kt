package model

import kotlinx.serialization.Serializable

@Serializable
data class LlistaDeLaCompra (
    val idLlista: Int,
    val nomLlista : String,
    val productes : List<Int>,
    val propietaris : List<Int>
)