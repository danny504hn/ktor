package com.example.plugins.routingV1
import com.example.model.request.PeticioLlista
import com.example.model.request.PeticioProducteDeLaLlista
import com.example.repositoris.RepositoriLlistaDeLaCompra
import com.example.repositoris.RepositoriProductesDeLaLlista
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import model.CampActualitzable
import model.toCampActualitzable

fun Route.rutesLlistes() {
    authenticate("auth-jwt") {
        route("llistes") {
            get {
                val idUsuari = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("idUsuari")?.asInt()
                    ?: return@get call.respond(status = HttpStatusCode.Unauthorized, message = "unauthorized")

                call.respond(RepositoriLlistaDeLaCompra.cercaLlistesPerPropietaris(idUsuari))
            }
            post {
                val idUsuari = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("idUsuari")?.asInt()
                    ?: return@post call.respond(status = HttpStatusCode.Unauthorized, message = "unauthorized")

                val parametres = call.receive<PeticioLlista>()
                if (parametres.nomLlista == null) {
                    return@post call.respond(status = HttpStatusCode.BadRequest, message = "Falta el nom de la llista")
                }
                val llistaCreada = RepositoriLlistaDeLaCompra.creaLlista(parametres.nomLlista, idUsuari)
                if (llistaCreada != null) {
                    call.respond(status = HttpStatusCode.Created, message = llistaCreada)
                } else {
                    call.respond(status = HttpStatusCode.BadRequest, message = "No s'ha pogut crear la llista")
                }
            }
            patch("{idLlista}") {
                val idLlista = call.parameters["idLlista"]?.toIntOrNull()
                    ?: return@patch call.respond(status = HttpStatusCode.BadRequest, message = "Id no valid")
                val parametres = call.receive<PeticioLlista>()
                RepositoriLlistaDeLaCompra.actualitzaLlista(
                    idLlista,
                    nouNomLlista = parametres.nomLlista.toCampActualitzable(),
                    idsPropietaris = parametres.idsPropietaris.toCampActualitzable()
                )
                call.respond(status = HttpStatusCode.OK, message = "Llista actualitzada")
            }
            delete("{idLlista}") {
                val idLlista = call.parameters["idLlista"]?.toIntOrNull()
                    ?: return@delete call.respond(status = HttpStatusCode.BadRequest, message = "Id no valid")
                val exit = RepositoriLlistaDeLaCompra.eliminaLlista(idLlista)
                if (exit) {
                    call.respond(status = HttpStatusCode.OK, message = "Llista eliminada")
                } else {
                    call.respond(status = HttpStatusCode.NotFound, message = "Llista no trobada")
                }
            }

            // Productes de la llista
            route("{idLlista}/productes") {
                get {
                    val idLlista = call.parameters["idLlista"]?.toIntOrNull()
                        ?: return@get call.respond(status = HttpStatusCode.BadRequest, message = "Id no valid")
                    call.respond(RepositoriProductesDeLaLlista.cercaProductesPerLlista(idLlista))
                }
                post {
                    val idLlista = call.parameters["idLlista"]?.toIntOrNull()
                        ?: return@post call.respond(status = HttpStatusCode.BadRequest, message = "Id no valid")
                    val idUsuari = call.principal<JWTPrincipal>()
                        ?.payload?.getClaim("idUsuari")?.asInt()
                        ?: return@post call.respond(status = HttpStatusCode.Unauthorized, message = "unauthorized")
                    val parametres = call.receive<PeticioProducteDeLaLlista>()
                    if (parametres.idProducte == null || parametres.nomProducte == null) {
                        return@post call.respond(status = HttpStatusCode.BadRequest, message = "Falten dades")
                    }
                    val producteCreat = RepositoriProductesDeLaLlista.creaProducteDeLaLlista(
                        idLlista = idLlista,
                        idProducte = parametres.idProducte,
                        nomProducte = parametres.nomProducte,
                        quantitat = parametres.quantitat ?: 1,
                        unitat = parametres.unitat ?: "unitats",
                        estatComprat = parametres.estaComprat ?: false,
                        quiHaComprat = null
                    )
                    if (producteCreat != null) {
                        call.respond(status = HttpStatusCode.Created, message = producteCreat)
                    } else {
                        call.respond(status = HttpStatusCode.BadRequest, message = "No s'ha pogut afegir el producte")
                    }
                }
                patch("{idProducte}") {
                    val idLlista = call.parameters["idLlista"]?.toIntOrNull()
                        ?: return@patch call.respond(status = HttpStatusCode.BadRequest, message = "Id llista no valid")
                    val idProducte = call.parameters["idProducte"]?.toIntOrNull()
                        ?: return@patch call.respond(status = HttpStatusCode.BadRequest, message = "Id producte no valid")
                    val parametres = call.receive<PeticioProducteDeLaLlista>()
                    val exit = RepositoriProductesDeLaLlista.actualitzaProducte(
                        idLlista = idLlista,
                        idProducte = idProducte,
                        nomProducte = parametres.nomProducte.toCampActualitzable(),
                        quantitat = parametres.quantitat.toCampActualitzable(),
                        unitat = parametres.unitat.toCampActualitzable(),
                        estatComprat = parametres.estaComprat.toCampActualitzable(),
                        quiHaComprat = parametres.quiHaComprat.toCampActualitzable()
                    )
                    if (exit) {
                        call.respond(status = HttpStatusCode.OK, message = "Producte actualitzat")
                    } else {
                        call.respond(status = HttpStatusCode.NotFound, message = "Producte no trobat")
                    }
                }
                delete("{idProducte}") {
                    val idLlista = call.parameters["idLlista"]?.toIntOrNull()
                        ?: return@delete call.respond(status = HttpStatusCode.BadRequest, message = "Id llista no valid")
                    val idProducte = call.parameters["idProducte"]?.toIntOrNull()
                        ?: return@delete call.respond(status = HttpStatusCode.BadRequest, message = "Id producte no valid")
                    val exit = RepositoriProductesDeLaLlista.eliminaProducte(idLlista, idProducte)
                    if (exit) {
                        call.respond(status = HttpStatusCode.OK, message = "Producte eliminat de la llista")
                    } else {
                        call.respond(status = HttpStatusCode.NotFound, message = "Producte no trobat a la llista")
                    }
                }
            }
        }
    }
}