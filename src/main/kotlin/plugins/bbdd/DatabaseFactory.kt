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
                Productes
            )
        }
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
                val nomsCategories = listOf("Fruita", "Neteja", "Carnisseria", "Congelats", "Begudes")

                nomsCategories.forEach { nomCat ->
                    val categoriaCreada = RepositoriCategories.creaCategoria(nomCat)

                    categoriaCreada?.let { cat ->
                        val numProds = (3..8).random()
                        for (i in 1..numProds) {
                            RepositoriProductes.creaProducte(
                                _nomProducte = "Producte ${cat.nomCategoria} $i",
                                _idCategoria = cat.id
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