package model

import kotlinx.serialization.Serializable

@Serializable
data class ProducteDeLaLlista (
    val idProducte : Int,
    val quantitat :  Int,
    val unitat : String,
    val estaComprat : Boolean,
    val quiHaComprat : Int
)