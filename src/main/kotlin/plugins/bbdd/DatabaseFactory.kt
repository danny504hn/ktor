package com.example.plugins.bbdd

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.statements.StatementType
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.collections.emptyList
import com.example.plugins.bbdd.Schema.Usuaris
import com.example.plugins.bbdd.Schema.Categories
import com.example.plugins.bbdd.Schema.Productes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import com.example.plugins.bbdd.Schema.LlistesDeLaCompra
import com.example.plugins.bbdd.Schema.ProductesDeLaLlista
import com.example.plugins.bbdd.Schema.LlistaPropietaris
import com.example.repositoris.RepositoriLlistaDeLaCompra
import com.example.repositoris.RepositoriProductesDeLaLlista
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import repositoris.RepositoriCategories
import repositoris.RepositoriProductes
import repositoris.RepositoriUsuaris

object DatabaseFactory {
    fun init() {
        val jdbcURL = "jdbc:sqlite:./llistes_de_la_compra.db"
        val database = Database.connect(jdbcURL, driver = "org.sqlite.JDBC")

        transaction(database) {
            exec("PRAGMA foreign_keys = ON;", emptyList(), StatementType.OTHER)

            SchemaUtils.create(
                Usuaris,
                Categories,
                Productes,
                LlistesDeLaCompra,
                ProductesDeLaLlista,
                LlistaPropietaris
            )
        }
        poblaLaBBDD()
    }

    fun poblaLaBBDD(){
        runBlocking{
            if (RepositoriUsuaris.obtenTots().isEmpty()) {
                RepositoriUsuaris.creaUsuari(
                    _nomUsuari = "Joan",
                    _password = "Joan",
                    _alias = null
                )
                RepositoriUsuaris.creaUsuari(
                    _nomUsuari = "Marta",
                    _password = "Marta",
                    _alias = null
                )
                RepositoriUsuaris.creaUsuari(
                    _nomUsuari = "Pere",
                    _password = "Pere",
                    _alias = "Perico"
                )
                RepositoriUsuaris.creaUsuari(
                    _nomUsuari = "Alex",
                    _password = "Alex",
                    _alias = "Aliexpres"
                )
                RepositoriUsuaris.creaUsuari(
                    _nomUsuari = "Fenrico",
                    _password = "Fenrico",
                    _alias = "Fenri"
                )
            }

            if (RepositoriCategories.obtenTotes().isEmpty()) {
                val nomsCategories = listOf("Neteja", "Fruita", "Carnisseria", "Congelats", "Begudes")

                nomsCategories.forEach { nomCat ->
                    val categoriaCreada = RepositoriCategories.creaCategoria(nomCat)

                    categoriaCreada?.let { cat ->
                        val numProds = (3..8).random()
                        for (i in 1..numProds) {
                            RepositoriProductes.creaProducte(
                                _nomProducte = "Producte TEST ${cat.nomCategoria} $i",
                                _idCategoria = cat.id
                            )
                        }
                    }
                }
            }
            // --- Hardcodeo de 10 Listas de la Compra ---
            val llistesExistents = RepositoriLlistaDeLaCompra.obtenTots()
            if (llistesExistents.isEmpty()) {
                // Obtenemos los IDs reales de los usuarios creados (Joan, Marta, Pere, Alex, Fenrico)
                val idsUsuaris = RepositoriUsuaris.obtenTots().map { it.id }

                if (idsUsuaris.isNotEmpty()) {
                    val dadesLlistes = listOf(
                        "🛒 Compra Mensual" to listOf(idsUsuaris[0]),
                        "🎉 Festa d'Aniversari" to listOf(idsUsuaris[0], idsUsuaris[1]),
                        "🧼 Neteja llar" to listOf(idsUsuaris[1]),
                        "🥩 Barbacoa Diumenge" to listOf(idsUsuaris[2], idsUsuaris[3]),
                        "🏢 Oficina" to listOf(idsUsuaris[4]),
                        "🍎 Sopar Vegà" to listOf(idsUsuaris[0], idsUsuaris[4]),
                        "⛺ Càmping Estiu" to listOf(idsUsuaris[2]),
                        "🏃 Gimnàs i Dieta" to listOf(idsUsuaris[1], idsUsuaris[3]),
                        "🍰 Receptek Pastís" to listOf(idsUsuaris[0]),
                        "📦 Bàsics de rebost" to listOf(idsUsuaris[3])
                    )

                    dadesLlistes.forEach { (nom, propietaris) ->
                        RepositoriLlistaDeLaCompra.creaLlista(nom, propietaris)
                    }
                }
                // --- Hardcodeo de 10 productos en las listas usando el Repositorio ---
                val productesLlistaExistents = RepositoriProductesDeLaLlista.obtenTots()

                if (productesLlistaExistents.isEmpty()) {
                    val llistes = RepositoriLlistaDeLaCompra.obtenTots()
                    val productesCataleg = RepositoriProductes.obtenTots()
                    val usuaris = RepositoriUsuaris.obtenTots()

                    if (llistes.isNotEmpty() && productesCataleg.isNotEmpty()) {
                        val unitatsMostra = listOf("kg", "unitats", "paquets", "grams", "botelles")

                        for (i in 1..10) {
                            val llistaAzar = llistes.random()
                            val prodAzar = productesCataleg.random()
                            val compratAzar = (i % 3 == 0)

                            // Usamos tu nuevo repositorio (ya no hace falta dbQuery aquí fuera)
                            RepositoriProductesDeLaLlista.creaProducteDeLaLlista(
                                idLlista = llistaAzar.idLlista,
                                idProducte = prodAzar.id,
                                nomProducte = prodAzar.nomProducte, // El repo lo pide aunque luego haga el join
                                quantitat = (1..5).random(),
                                unitat = unitatsMostra.random(),
                                estatComprat = compratAzar,
                                quiHaComprat = if (compratAzar) usuaris.random().id else null
                            )
                        }
                    }
                }
            }



        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) {
            block()
        }
}