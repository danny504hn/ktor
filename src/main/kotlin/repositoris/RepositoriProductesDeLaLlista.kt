package com.example.repositoris

import com.example.plugins.bbdd.DatabaseFactory.dbQuery
import model.CampActualitzable
import model.ProducteDeLaLlista
import com.example.plugins.bbdd.Schema.ProductesDeLaLlista
import com.example.plugins.bbdd.Schema.Productes
import io.ktor.server.html.insert
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

object RepositoriProductesDeLaLlista {

    suspend fun creaProducteDeLaLlista(
        idLlista: Int,
        idProducte: Int,
        nomProducte: String,
        quantitat: Int,
        unitat: String,
        estatComprat: Boolean,
        quiHaComprat: Int?
    ): ProducteDeLaLlista? = dbQuery {
        val insertId = ProductesDeLaLlista.insert {
            it[ProductesDeLaLlista.idLlista] = idLlista
            it[ProductesDeLaLlista.idProducte] = idProducte
            it[ProductesDeLaLlista.quantitat] = quantitat
            it[ProductesDeLaLlista.unitat] = unitat
            it[ProductesDeLaLlista.estaComprat] = estatComprat
            it[ProductesDeLaLlista.quiHaComprat] = quiHaComprat
        } get ProductesDeLaLlista.id
        (ProductesDeLaLlista innerJoin Productes)
            .selectAll()
            .where { ProductesDeLaLlista.id eq insertId }
            .singleOrNull()
            ?.toProducteDeLaLlista()
    }

    suspend fun cercaProductePerId(idLlista: Int, idProducte: Int): ProducteDeLaLlista? = dbQuery {
        (ProductesDeLaLlista innerJoin Productes)
            .selectAll()
            .where {
                (ProductesDeLaLlista.idLlista eq idLlista) and
                        (ProductesDeLaLlista.idProducte eq idProducte)
            }
            .singleOrNull()
            ?.toProducteDeLaLlista()
    }

    suspend fun cercaProductesPerLlista(idLlista: Int): List<ProducteDeLaLlista> = dbQuery {
        (ProductesDeLaLlista innerJoin Productes)
            .selectAll()
            .where { ProductesDeLaLlista.idLlista eq idLlista }
            .map { it.toProducteDeLaLlista() }
    }

    suspend fun cercaProductesPerLlistaNoComprats(idLlista: Int): List<ProducteDeLaLlista> = dbQuery {
        (ProductesDeLaLlista innerJoin Productes)
            .selectAll()
            .where {
                (ProductesDeLaLlista.idLlista eq idLlista) and
                        (ProductesDeLaLlista.estaComprat eq false)
            }
            .map { it.toProducteDeLaLlista() }
    }

    suspend fun cercaProductesPerLlistaComprats(idLlista: Int): List<ProducteDeLaLlista> = dbQuery {
        (ProductesDeLaLlista innerJoin Productes)
            .selectAll()
            .where {
                (ProductesDeLaLlista.idLlista eq idLlista) and
                        (ProductesDeLaLlista.estaComprat eq true)
            }
            .map { it.toProducteDeLaLlista() }
    }

    suspend fun obtenTots(): List<ProducteDeLaLlista> = dbQuery {
        (ProductesDeLaLlista innerJoin Productes)
            .selectAll()
            .map { it.toProducteDeLaLlista() }
    }

    suspend fun actualitzaNomProducte(idLlista: Int, idProducte: Int, nom: String): Boolean = dbQuery {
        val files = Productes.update({ Productes.id eq idProducte }) {
            it[nomProducte] = nom
        }
        files > 0
    }

    suspend fun actualitzaQuantitat(idLlista: Int, idProducte: Int, quantitat: Int): Boolean = dbQuery {
        val files = ProductesDeLaLlista.update({
            (ProductesDeLaLlista.idLlista eq idLlista) and
                    (ProductesDeLaLlista.idProducte eq idProducte)
        }) {
            it[ProductesDeLaLlista.quantitat] = quantitat
        }
        files > 0
    }

    suspend fun actualitzaUnitat(idLlista: Int, idProducte: Int, unitat: String): Boolean = dbQuery {
        val files = ProductesDeLaLlista.update({
            (ProductesDeLaLlista.idLlista eq idLlista) and
                    (ProductesDeLaLlista.idProducte eq idProducte)
        }) {
            it[ProductesDeLaLlista.unitat] = unitat
        }
        files > 0
    }

    suspend fun actualitzaEstatCompra(idLlista: Int, idProducte: Int, estatCompra: Boolean): Boolean = dbQuery {
        val files = ProductesDeLaLlista.update({
            (ProductesDeLaLlista.idLlista eq idLlista) and
                    (ProductesDeLaLlista.idProducte eq idProducte)
        }) {
            it[estaComprat] = estatCompra
        }
        files > 0
    }

    suspend fun actualitzaProducte(
        idLlista: Int,
        idProducte: Int,
        nomProducte: CampActualitzable<String> = CampActualitzable.SenseCanvi,
        quantitat: CampActualitzable<Int> = CampActualitzable.SenseCanvi,
        unitat: CampActualitzable<String> = CampActualitzable.SenseCanvi,
        estatComprat: CampActualitzable<Boolean> = CampActualitzable.SenseCanvi,
        quiHaComprat: CampActualitzable<Int?> = CampActualitzable.SenseCanvi
    ): Boolean = dbQuery {
        if (nomProducte is CampActualitzable.NouValor) {
            Productes.update({ Productes.id eq idProducte }) {
                it[Productes.nomProducte] = nomProducte.valor
            }
        }

        // Actualitzem la resta a ProductesDeLaLlista
        val files = ProductesDeLaLlista.update({
            (ProductesDeLaLlista.idLlista eq idLlista) and
                    (ProductesDeLaLlista.idProducte eq idProducte)
        }) {
            if (quantitat is CampActualitzable.NouValor) it[ProductesDeLaLlista.quantitat] = quantitat.valor
            if (unitat is CampActualitzable.NouValor) it[ProductesDeLaLlista.unitat] = unitat.valor
            if (estatComprat is CampActualitzable.NouValor) it[ProductesDeLaLlista.estaComprat] = estatComprat.valor
            if (quiHaComprat is CampActualitzable.NouValor) it[ProductesDeLaLlista.quiHaComprat] = quiHaComprat.valor
        }
        files > 0
    }

    suspend fun eliminaProducte(idLlista: Int, idProducte: Int): Boolean = dbQuery {
        val files = ProductesDeLaLlista.deleteWhere {
            (ProductesDeLaLlista.idLlista eq idLlista) and
                    (ProductesDeLaLlista.idProducte eq idProducte)
        }
        files > 0
    }

    suspend fun eliminaProductesPerLlista(idLlista: Int): Boolean = dbQuery {
        val files = ProductesDeLaLlista.deleteWhere {
            (ProductesDeLaLlista.idLlista eq idLlista) and
                    (ProductesDeLaLlista.idProducte eq idProducte)
        }
        files > 0
    }

    fun ResultRow.toProducteDeLaLlista(): ProducteDeLaLlista {
        return ProducteDeLaLlista(
            idProducte = this[ProductesDeLaLlista.idProducte],
            quantitat = this[ProductesDeLaLlista.quantitat],
            unitat = this[ProductesDeLaLlista.unitat] ?: "",
            estaComprat = this[ProductesDeLaLlista.estaComprat],
            quiHaComprat = this[ProductesDeLaLlista.quiHaComprat] ?: 0
        )
    }
}