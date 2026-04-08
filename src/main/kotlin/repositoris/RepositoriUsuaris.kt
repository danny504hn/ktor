package repositoris

import com.example.plugins.bbdd.DatabaseFactory.dbQuery
import com.example.plugins.bbdd.Schema.Usuaris
import model.CampActualitzable
import model.Usuari
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

object RepositoriUsuaris {
    suspend fun creaUsuari(
        _nomUsuari: String,
        _password: String,
        _alias: String?
    ): Usuari? = dbQuery {
        val insercio = Usuaris.insert {
            it[nomUsuari] = _nomUsuari
            it[password] = _password
            it[alias] = _alias
        }

        insercio.resultedValues?.singleOrNull()?.toUsuari()
        }
    suspend fun cercaUsuariPerID(_id: Int): Usuari? = dbQuery{
        Usuaris.selectAll()
            .where{ Usuaris.id eq _id }
            .singleOrNull()
            ?.toUsuari()
    }

    suspend fun obtenTots(): List<Usuari> = dbQuery{
        Usuaris.selectAll()
            .map{ it.toUsuari() }
    }

    suspend fun eliminarUsuari(_id:Int) = dbQuery{
        Usuaris.deleteWhere{
            Usuaris.id eq _id
        } > 0
    }

    suspend fun actualitzaAliasUsuari(_id: Int, _alias: String?) = dbQuery{
        Usuaris.update(where = { Usuaris.id eq _id }){
            it[Usuaris.alias] = _alias
        }
    }

    suspend fun actualitzaNomUsuari(_id: Int, _nom: String) = dbQuery{
        Usuaris.update(where = { Usuaris.id eq _id }){
            it[Usuaris.nomUsuari] = _nom
        }
    }

    suspend fun actualitzaUsuari(
        _id:Int,
        _nom:CampActualitzable<String>,
        _password: CampActualitzable<String>,
        _alias: CampActualitzable<String>
    ) = dbQuery {
        Usuaris.update(where = {Usuaris.id eq _id}){
            if (_nom is CampActualitzable.NouValor)
                it[Usuaris.nomUsuari] = _nom.valor
            if (_password is CampActualitzable.NouValor)
                it[Usuaris.password] = _password.valor
            if (_alias is CampActualitzable.NouValor)
                it[Usuaris.alias] = _alias.valor
        }
    }


    suspend fun actualitzaPasswordUsuari(_id: Int, _password: String) = dbQuery{
        Usuaris.update(where = { Usuaris.id eq _id }){
            it[Usuaris.password] = _password
        }
    }

    private fun ResultRow.toUsuari(): Usuari = Usuari(id = this[Usuaris.id],
        nomUsuari = this[Usuaris.nomUsuari],
        password = this[Usuaris.password],
        alias = this[Usuaris.alias]
    )
}
