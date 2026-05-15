package com.example.plugins.routingV1

import com.example.model.websockets.TipusAccio
import kotlinx.serialization.Serializable
import model.ProducteDeLaLlista
@Serializable
data class EsdevenimentLlista(
    val accio: TipusAccio,
    val idLlista: Int? = null,
    val idRecursAfectat: Int,
    val producte: ProducteDeLaLlista?,
    val timeStamp: Long = System.currentTimeMillis()
)
