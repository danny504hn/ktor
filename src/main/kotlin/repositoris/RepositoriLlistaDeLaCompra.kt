package com.example.repositoris

import com.example.plugins.bbdd.DatabaseFactory.dbQuery
import com.example.plugins.bbdd.Schema
import com.example.plugins.bbdd.Schema.LlistaPropietaris
import com.example.plugins.bbdd.Schema.LlistesDeLaCompra
import model.CampActualitzable
import org.jetbrains.exposed.sql.insert
import model.LlistaDeLaCompra
import org.jetbrains.exposed.sql.ISqlExpressionBuilder
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import kotlin.String

object RepositoriLlistaDeLaCompra {

    suspend fun creaLlista(_nomLlista : String, _idPropietari: Int): LlistaDeLaCompra? =
        creaLlista(_nomLlista, listOf<Int>(_idPropietari))

    suspend fun creaLlista(_nomLlista: String, _idsPropietaris: List<Int> ):LlistaDeLaCompra? = dbQuery {
        val insercio = LlistesDeLaCompra.insert {
            it[nomLlista] = _nomLlista
        }

        val id = insercio[LlistesDeLaCompra.id]
        _idsPropietaris.forEach { propietari ->
            LlistaPropietaris.insert {
                it[idLlista] = id
                it[idPropietari] = propietari
            }
        }

        LlistaDeLaCompra(
            idLlista = id,
            nomLlista = _nomLlista,
            productes = emptyList(),
            propietaris = _idsPropietaris
        )
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

    suspend fun cercaLlistesPerPropietaris(idPropietari: Int): List<LlistaDeLaCompra> = dbQuery {
        (LlistesDeLaCompra innerJoin Schema.LlistaPropietaris)
            .select(LlistesDeLaCompra.columns)
            .where { Schema.LlistaPropietaris.idPropietari eq idPropietari}
            .map { row ->
                val propietaris = LlistaPropietaris
                    .selectAll()
                    .where{ LlistaPropietaris.idLlista eq row[LlistesDeLaCompra.id] }
                    .map{ it[Schema.LlistaPropietaris.idPropietari]}
                LlistaDeLaCompra(
                    idLlista = row[LlistesDeLaCompra.id],
                    nomLlista = row[LlistesDeLaCompra.nomLlista],
                    productes = emptyList(),
                    propietaris = emptyList()
                )
            }
    }

    suspend fun obtenTots(): List<LlistaDeLaCompra> = dbQuery {
        LlistesDeLaCompra.selectAll()
            .map { it.toLlistaDeLaCompra() }
    }

    suspend fun actualitzaPropietariLlista(idLlista: Int, idUsuari : Int)  : Boolean = dbQuery {
        var actualitzat : Boolean = false;
        val llistaActual = LlistesDeLaCompra
            .selectAll()
            .where { LlistesDeLaCompra.id eq idLlista }
            .singleOrNull()

       if(llistaActual != null){
           LlistaPropietaris.update(where = {
               LlistaPropietaris.idLlista eq idLlista
           }){
               it[idPropietari] = idUsuari
           }

       }
        return@dbQuery actualitzat
    }

    suspend fun actualitzaPropietarisLlista(idLlista: Int, idsUsuaris: List<Int> ): Boolean = dbQuery {
        var actualitzat : Boolean = false;
        val llista = LlistesDeLaCompra.selectAll()
            .where{ LlistesDeLaCompra.id eq idLlista }
            .singleOrNull()

        if(llista != null){


            LlistaPropietaris.deleteWhere { LlistaPropietaris.idLlista eq idLlista }

            idsUsuaris.forEach { propietari ->
                LlistaPropietaris.insert {
                    it[this.idLlista] = idLlista
                    it[this.idPropietari] = propietari
                }
            }
            actualitzat = true;
        }

        return@dbQuery actualitzat
    }
    suspend fun actualitzaLlista(id: Int,
                                 nouNomLlista: CampActualitzable<String> = CampActualitzable.SenseCanvi,
                                 idsPropietaris: CampActualitzable<List<Int>> = CampActualitzable.SenseCanvi) = dbQuery {
        LlistesDeLaCompra.update({ LlistesDeLaCompra.id eq id }) {
            if (nouNomLlista is CampActualitzable.NouValor){
                it[nomLlista] = nouNomLlista.valor
            }
        }
        if(idsPropietaris is CampActualitzable.NouValor){
            actualitzaPropietarisLlista(id, idsPropietaris.valor)
        }

    }

    suspend fun eliminaLlista(id: Int): Boolean = dbQuery{
        LlistaPropietaris.deleteWhere { LlistaPropietaris.idLlista eq id }
        val files = LlistesDeLaCompra.deleteWhere{ LlistesDeLaCompra.id eq id}
        files > 0
    }

    private fun obtenIdsPropietaris(idLlista: Int): List<Int> {
        return LlistaPropietaris
            .selectAll()
            .where { LlistaPropietaris.idLlista eq idLlista }
            .map { it[LlistaPropietaris.idPropietari] }
    }

    private fun ResultRow.toLlistaDeLaCompra(): LlistaDeLaCompra = LlistaDeLaCompra(
        idLlista = this[LlistesDeLaCompra.id],
        nomLlista = this[LlistesDeLaCompra.nomLlista],
        productes = emptyList(),
        propietaris = emptyList()
    )
}