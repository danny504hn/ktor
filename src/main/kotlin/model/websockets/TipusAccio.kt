package com.example.model.websockets

import kotlinx.serialization.KSerializer;
import kotlinx.serialization.Serializable;

@Serializable
enum class TipusAccio {
    CONNEXIO_ESTABLERTA,
    PRODUCTe_AFEGIT,
    PRODUCTE_ACTUALITZAT,
    PRODUCTE_ELIMINAT,
    LLISTA_ACTUALITZADA,
    NOTIFICACIO_AMISTAT_NOVA,
    NOTIFICACIO_AMISTAT_ELIMINADA;

    companion object {
    }
}