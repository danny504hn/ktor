package repositoris

import Categoria
import com.example.plugins.bbdd.DatabaseFactory.dbQuery
import com.example.plugins.bbdd.Schema.Categories
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

object RepositoriCategories {

    suspend fun creaCategoria(_nomCategoria: String): Categoria? = dbQuery {
        val insercio = Categories.insert {
            it[nomCategoria] = _nomCategoria
        }
        insercio.resultedValues?.singleOrNull()?.toCategoria()
    }

    suspend fun cercaCategoriaPerId(_id: Int): Categoria? = dbQuery {
        Categories.selectAll()
            .where { Categories.id eq _id }
            .singleOrNull()
            ?.toCategoria()
    }

    suspend fun cercaCategoriaPerNom(_nomCategoria: String): Categoria? = dbQuery {
        Categories.selectAll()
            .where { Categories.nomCategoria eq _nomCategoria }
            .singleOrNull()
            ?.toCategoria()
    }

    suspend fun obtenTotes(): List<Categoria> = dbQuery {
        Categories.selectAll()
            .map { it.toCategoria() }
    }

    suspend fun actualitzaNomCategoria(_id: Int, _nomCategoria: String): Boolean = dbQuery {
        Categories.update(where = { Categories.id eq _id }) {
            it[nomCategoria] = _nomCategoria
        } > 0
    }

    suspend fun eliminaCategoria(_id: Int): Boolean = dbQuery {
        Categories.deleteWhere {
            Categories.id eq _id
        } > 0
    }

    private fun ResultRow.toCategoria(): Categoria = Categoria(
        id = this[Categories.id],
        nomCategoria = this[Categories.nomCategoria]
    )
}