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
                    productes = row[LlistesDeLaCompra.productes].toList(),
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
            val propietarisLlista = llistaActual[LlistesDeLaCompra.propietaris].toMutableList()
            if(!propietarisLlista.contains(idUsuari)){
                propietarisLlista.add(idUsuari)

                LlistesDeLaCompra.update({ LlistesDeLaCompra.id eq idLlista }) {
                    it[propietaris] = propietarisLlista
                } > 0
                actualitzat = true;
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

            LlistesDeLaCompra.update({ LlistesDeLaCompra.id eq idLlista }) {
                it[propietaris] = idsUsuaris
            }
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
            if(idsPropietaris is CampActualitzable.NouValor){
                it[propietaris] = idsPropietaris.valor
            }
            if (nouNomLlista is CampActualitzable.NouValor){
                it[nomLlista] = nouNomLlista.valor
            }
        }

    }

    suspend fun eliminaLlista(id: Int): Boolean = dbQuery{
        LlistaPropietaris.deleteWhere { LlistaPropietaris.idLlista eq id }
        val files = LlistesDeLaCompra.deleteWhere{ LlistesDeLaCompra.id eq id}
        files > 0
    }

    private fun ResultRow.toLlistaDeLaCompra(): LlistaDeLaCompra = LlistaDeLaCompra(
        idLlista = this[LlistesDeLaCompra.id],
        nomLlista = this[LlistesDeLaCompra.nomLlista],
        productes = this[LlistesDeLaCompra.productes],
        propietaris = this[LlistesDeLaCompra.propietaris]
    )
}