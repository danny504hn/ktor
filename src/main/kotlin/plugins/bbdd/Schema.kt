package com.example.plugins.bbdd

import org.jetbrains.exposed.sql.Table

object Schema {

    object Usuaris: Table("Usuaris"){
        val id = integer("id").autoIncrement()
        val alias = varchar("alias", 30).nullable()
        val nomUsuari = varchar("nom_usuari", 50)
        val password = varchar("password", 128)

        override val primaryKey = PrimaryKey(id)
    }

    object Categories: Table("Categories"){
        val id = integer("id").autoIncrement()
        val nomCategoria = varchar("nom_categoria", 50)

        override val primaryKey = PrimaryKey(id)
    }

    object Productes: Table("Productes"){
        val id = integer("id").autoIncrement()
        val nomProducte = varchar("nom_producte", 50)
        val idCategoria = integer("categoria_id").references(Categories.id)

        override val primaryKey = PrimaryKey(id)
    }

    object LlistesDeLaCompra: Table("LlistesDeCompra"){
        val id = integer("id").autoIncrement()
        val nomLlista = varchar("nom_llista", 50)
        val productes = array<Int>("productes")
        val propietaris = array<Int>("propietaris")

        override val primaryKey = PrimaryKey(id)
    }

    object LlistaPropietaris: Table("LlistaPropertaris"){
        val idLlista = integer("idLlista").references(LlistesDeLaCompra.id)
        val idPropietari = integer("idPropietaris").references(Usuaris.id)

        override val primaryKey = PrimaryKey(idLlista, idPropietari)
    }

    object ProductesDeLaLlista : Table("ProductesDeLaLlista"){
        val id = integer("id").autoIncrement()
        val idLlista = integer("idLlista").references(LlistesDeLaCompra.id)
        val idProducte = integer("idProducte").references(Productes.id)
        val quantitat = integer("quantitat")
        val unitat = varchar("unitat", 30).nullable()
        val estaComprat = bool("estaComprat")
        val quiHaComprat = integer("quiHaComprat").nullable()

        override val primaryKey = PrimaryKey(id)
    }

    

}