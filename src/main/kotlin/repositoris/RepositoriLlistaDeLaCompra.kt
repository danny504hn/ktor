package com.example.repositoris

import com.example.plugins.bbdd.DatabaseFactory.dbQuery
import com.example.plugins.bbdd.Schema.LlistesDeLaCompra
import com.example.plugins.bbdd.Schema.Productes
import org.jetbrains.exposed.sql.insert
import model.LlistaDeLaCompra
import model.Producte
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import repositoris.RepositoriProductes.toProducte
import kotlin.String

object RepositoriLlistaDeLaCompra {

    suspend fun creaLlista(_nomLlista : String, _idPropietari: Int): LlistaDeLaCompra? = dbQuery {
        val insercio = LlistesDeLaCompra.insert{
            it[nomLlista] = _nomLlista
            it[propietaris] = listOf(_idPropietari)
        }
        insercio.resultedValues?.singleOrNull()?.toLlistaDeLaCompra()
    }
    suspend fun creaLlista(_nomLlista: String, _idsPropietaris: List<Int> ):LlistaDeLaCompra? = dbQuery {
        val insercio = LlistesDeLaCompra.insert {
            it[nomLlista] = _nomLlista
            it[propietaris] = _idsPropietaris
        }
        insercio.resultedValues?.singleOrNull()?.toLlistaDeLaCompra()
    }

    suspend fun cercaLlistaPerId(id : Int) :LlistaDeLaCompra? = dbQuery {
        LlistesDeLaCompra.selectAll()
            .where { LlistesDeLaCompra.id eq id }
            .singleOrNull()
            ?.toLlistaDeLaCompra()
    }

    suspend fun cercaLlistaPerNom(nomLlista : String) : LlistaDeLaCompra? = dbQuery {
        LlistesDeLaCompra.selectAll()
            .where{ LlistesDeLaCompra.nomLlista eq nomLlista }
            .singleOrNull()
            ?.toLlistaDeLaCompra()
    }

    //LLISTA PROPIETARI, BUSCAS PROPIETARI, Y DEVUELVES TODAS LAS LISTAS A LAS QUE PERTENECE
    suspend fun cercaLlistesPerPropietaris(idPropietari: Int): List<Producte> = dbQuery {
       null
    }

    suspend fun obtenTots(): List<LlistaDeLaCompra> = dbQuery {
        LlistesDeLaCompra.selectAll()
            .map { it.toLlistaDeLaCompra() }
    }

    private fun ResultRow.toLlistaDeLaCompra(): LlistaDeLaCompra = LlistaDeLaCompra(
        idLlista = this[LlistesDeLaCompra.id],
        nomLlista = this[LlistesDeLaCompra.nomLlista],
        productes = this[LlistesDeLaCompra.productes],
        propietaris = this[LlistesDeLaCompra.propietaris]
    )
}