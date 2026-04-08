package repositoris

import cat.montilivi.model.model.Producte
import com.example.plugins.bbdd.DatabaseFactory.dbQuery
import com.example.plugins.bbdd.Schema.Productes
import model.CampActualitzable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

object RepositoriProductes {

    suspend fun creaProducte(_nomProducte: String, _idCategoria: Int): Producte? = dbQuery {
        val insercio = Productes.insert {
            it[nomProducte] = _nomProducte
            it[idCategoria] = _idCategoria
        }
        insercio.resultedValues?.singleOrNull()?.toProducte()
    }

    suspend fun cercaProductePerId(_id: Int): Producte? = dbQuery {
        Productes.selectAll()
            .where { Productes.id eq _id }
            .singleOrNull()
            ?.toProducte()
    }

    suspend fun cercaProductePerNom(_nomProducte: String): Producte? = dbQuery {
        Productes.selectAll()
            .where { Productes.nomProducte eq _nomProducte }
            .singleOrNull()
            ?.toProducte()
    }

    suspend fun cercaProductesPerCategoria(_idCategoria: Int): List<Producte> = dbQuery {
        Productes.selectAll()
            .where { Productes.idCategoria eq _idCategoria }
            .map { it.toProducte() }
    }

    suspend fun obtenTots(): List<Producte> = dbQuery {
        Productes.selectAll()
            .map { it.toProducte() }
    }

    suspend fun actualitzaNomProducte(_id: Int, _nomProducte: String): Boolean = dbQuery {
        Productes.update(where = { Productes.id eq _id }) {
            it[nomProducte] = _nomProducte
        } > 0
    }

    suspend fun actualitzaCategoriaProducte(_id: Int, _idCategoria: Int): Boolean = dbQuery {
        Productes.update(where = { Productes.id eq _id }) {
            it[idCategoria] = _idCategoria
        } > 0
    }

    suspend fun actualitzaProducte(
        _id: Int,
        _nomProducte: CampActualitzable<String> = CampActualitzable.SenseCanvi,
        _idCategoria: CampActualitzable<Int> = CampActualitzable.SenseCanvi
    ): Boolean = dbQuery {
        Productes.update(where = { Productes.id eq _id }) {
            if (_nomProducte is CampActualitzable.NouValor)
                it[nomProducte] = _nomProducte.valor
            if (_idCategoria is CampActualitzable.NouValor)
                it[idCategoria] = _idCategoria.valor
        } > 0
    }

    suspend fun eliminaProducte(_id: Int): Boolean = dbQuery {
        Productes.deleteWhere {
            Productes.id eq _id
        } > 0
    }

    private fun ResultRow.toProducte(): Producte = Producte(
        id = this[Productes.id],
        nomProducte = this[Productes.nomProducte],
        idCategoria = this[Productes.idCategoria]
    )
}