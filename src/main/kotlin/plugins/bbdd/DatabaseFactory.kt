package com.example.plugins.bbdd

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.statements.StatementType
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.collections.emptyList
import com.example.plugins.bbdd.Schema.Usuaris
import com.example.plugins.bbdd.Schema.Categories
import com.example.plugins.bbdd.Schema.Productes
import com.example.plugins.bbdd.Schema.UsuariAmics
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
                LlistaPropietaris,
                UsuariAmics
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
                RepositoriUsuaris.afegeixAmic(1,2)
                RepositoriUsuaris.afegeixAmic(2,3)
                RepositoriUsuaris.afegeixAmic(3,4)
                RepositoriUsuaris.afegeixAmic(4,5)
                RepositoriUsuaris.afegeixAmic(2,1)
                RepositoriUsuaris.afegeixAmic(3,2)
                RepositoriUsuaris.afegeixAmic(4,3)
                RepositoriUsuaris.afegeixAmic(5,4)
                RepositoriUsuaris.afegeixComAPropietariAUnaLlista(2,1)
            }


            if (RepositoriCategories.obtenTotes().isEmpty()) {
                val nomsCategories = listOf("Neteja", "Fruita", "Carnisseria", "Congelats", "Begudes")

                nomsCategories.forEach { nomCat ->
                    var categoriaCreada = RepositoriCategories.creaCategoria(nomCat)
                    var i = 0;
                    if(categoriaCreada != null){
                        i++
                        RepositoriProductes.creaProducte(
                            "Producte ${categoriaCreada.nomCategoria} $i",
                            categoriaCreada.id
                        )
                    }
                }
            }

            val llistesExistents = RepositoriLlistaDeLaCompra.obtenTots()
            if (llistesExistents.isEmpty()) {

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


                            RepositoriProductesDeLaLlista.creaProducteDeLaLlista(
                                idLlista = llistaAzar.idLlista,
                                idProducte = prodAzar.id,
                                nomProducte = prodAzar.nomProducte,
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