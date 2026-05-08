package com.example.plugins.routingV1

import com.example.model.websockets.TipusAccio
import model.ProducteDeLaLlista

data class EsdevenimentLlista(
    val accio: TipusAccio,
    val idLlista: Int? = null,
    val idRecursAfectat: Int,
    val producte: ProducteDeLaLlista?,
    val timeStamp: Long = System.currentTimeMillis()
)
