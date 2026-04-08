package model

import kotlinx.serialization.Serializable

@Serializable
data class Usuari(
    val id:Int,
    val alias:String?,
    val nomUsuari:String,
    val password: String
)