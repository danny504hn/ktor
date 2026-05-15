package com.example.plugins.routingV1
import com.example.model.request.PeticioProducte
import com.example.repositoris.RepositoriLlistaDeLaCompra
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import model.toCampActualitzable
import repositoris.RepositoriProductes

fun Route.rutesProductes() {
    authenticate("auth-jwt") {

        suspend fun obtenPropietarisANotificar(idLlista: Int, idUsuari: Int): List<Int> {
            val llista = RepositoriLlistaDeLaCompra.cercaLlistaPerId(idLlista)
            return llista?.propietaris?.filter { it != idUsuari } ?: emptyList()
        }

        route("productes") {
            get {
                val idCategoria = call.request.queryParameters["idCategoria"]?.toIntOrNull()
                if (idCategoria != null) {
                    call.respond(RepositoriProductes.cercaProductesPerCategoria(idCategoria))
                } else {
                    call.respond(RepositoriProductes.obtenTots())
                }
            }
            post {
                val parametres = call.receive<PeticioProducte>()
                if (parametres.nomProducte == null || parametres.idCategoria == null) {
                    return@post call.respond(status = HttpStatusCode.BadRequest, message = "Falten dades")
                }
                val producteCreat = RepositoriProductes.creaProducte(parametres.nomProducte, parametres.idCategoria)
                if (producteCreat != null) {
                    call.respond(status = HttpStatusCode.Created, message = producteCreat)
                } else {
                    call.respond(status = HttpStatusCode.BadRequest, message = "No s'ha pogut crear el producte")
                }
            }
            patch("{idProducte}") {
                val idProducte = call.parameters["idProducte"]?.toIntOrNull()
                    ?: return@patch call.respond(status = HttpStatusCode.BadRequest, message = "Id no valid")
                val parametres = call.receive<PeticioProducte>()
                val exit = RepositoriProductes.actualitzaProducte(
                    idProducte,
                    _nomProducte = parametres.nomProducte.toCampActualitzable(),
                    _idCategoria = parametres.idCategoria.toCampActualitzable()
                )
                if (exit) {
                    call.respond(status = HttpStatusCode.OK, message = "Producte actualitzat")
                } else {
                    call.respond(status = HttpStatusCode.NotFound, message = "Producte no trobat")
                }
            }
            delete("{idProducte}") {
                val idProducte = call.parameters["idProducte"]?.toIntOrNull()
                    ?: return@delete call.respond(status = HttpStatusCode.BadRequest, message = "Id no valid")
                val exit = RepositoriProductes.eliminaProducte(idProducte)

                if (exit) {
                    call.respond(status = HttpStatusCode.OK, message = "Producte eliminat")
                } else {
                    call.respond(status = HttpStatusCode.NotFound, message = "Producte no trobat o esta en una llista")
                }
            }
        }
    }
}