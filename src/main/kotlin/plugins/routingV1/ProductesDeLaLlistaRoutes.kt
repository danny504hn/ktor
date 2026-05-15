package com.example.plugins.routingV1
import com.example.model.request.PeticioLlista
import com.example.model.request.PeticioProducteDeLaLlista
import com.example.model.websockets.TipusAccio
import com.example.repositoris.RepositoriLlistaDeLaCompra
import com.example.repositoris.RepositoriProductesDeLaLlista
import com.example.utils.GestorDeConnexions
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
import repositoris.RepositoriUsuaris

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
                val idUsuari = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("idUsuari")?.asInt()
                    ?: return@patch call.respond(status = HttpStatusCode.Unauthorized, message = "unauthorized")
                val parametres = call.receive<PeticioLlista>()

                // Obtenim propietaris ABANS de modificar
                val llista = RepositoriLlistaDeLaCompra.cercaLlistaPerId(idLlista)
                println("Llista: $llista")
                println("Propietaris: ${llista?.propietaris}")
                println("IdUsuari actual: $idUsuari")



                RepositoriLlistaDeLaCompra.actualitzaLlista(
                    idLlista,
                    nouNomLlista = parametres.nomLlista.toCampActualitzable(),
                    idsPropietaris = parametres.idsPropietaris.toCampActualitzable()
                )
                call.respond(status = HttpStatusCode.OK, message = "Llista actualitzada")

                val aNotificar = llista?.propietaris?.filter { it != idUsuari } ?: emptyList()
                println("A notificar: $aNotificar")
                if (aNotificar.isNotEmpty()) {
                    GestorDeConnexions.enviaAUsuarisConcrects(
                        idsUsuaris = aNotificar,
                        esdevenimet = EsdevenimentLlista(
                            accio = TipusAccio.LLISTA_ACTUALITZADA,
                            idLlista = idLlista,
                            idRecursAfectat = idLlista,
                            producte = null
                        )
                    )
                }
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
                        val llista = RepositoriLlistaDeLaCompra.cercaLlistaPerId(idLlista)
                        val aNotificar = llista?.propietaris?.filter { it != idUsuari } ?: emptyList()
                        if (aNotificar.isNotEmpty()) {
                            GestorDeConnexions.enviaAUsuarisConcrects(
                                idsUsuaris = aNotificar,
                                esdevenimet = EsdevenimentLlista(
                                    accio = TipusAccio.PRODUCTE_AFEGIT,
                                    idLlista = idLlista,
                                    idRecursAfectat = producteCreat.idProducte,
                                    producte = producteCreat
                                )
                            )
                        }
                    } else {
                        call.respond(status = HttpStatusCode.BadRequest, message = "No s'ha pogut afegir el producte")
                    }
                }
                patch("{idProducte}") {
                    val idLlista = call.parameters["idLlista"]?.toIntOrNull()
                        ?: return@patch call.respond(status = HttpStatusCode.BadRequest, message = "Id llista no valid")
                    val idProducte = call.parameters["idProducte"]?.toIntOrNull()
                        ?: return@patch call.respond(status = HttpStatusCode.BadRequest, message = "Id producte no valid")
                    val idUsuari = call.principal<JWTPrincipal>()
                        ?.payload?.getClaim("idUsuari")?.asInt()
                        ?: return@patch call.respond(status = HttpStatusCode.Unauthorized, message = "unauthorized")
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
                        val llista = RepositoriLlistaDeLaCompra.cercaLlistaPerId(idLlista)
                        val aNotificar = llista?.propietaris?.filter { it != idUsuari } ?: emptyList()
                        if (aNotificar.isNotEmpty()) {
                            GestorDeConnexions.enviaAUsuarisConcrects(
                                idsUsuaris = aNotificar,
                                esdevenimet = EsdevenimentLlista(
                                    accio = TipusAccio.PRODUCTE_ACTUALITZAT,
                                    idLlista = idLlista,
                                    idRecursAfectat = idProducte,
                                    producte = null
                                )
                            )
                        }
                    } else {
                        call.respond(status = HttpStatusCode.NotFound, message = "Producte no trobat")
                    }
                }
                delete("{idProducte}") {
                    val idLlista = call.parameters["idLlista"]?.toIntOrNull()
                        ?: return@delete call.respond(status = HttpStatusCode.BadRequest, message = "Id llista no valid")
                    val idProducte = call.parameters["idProducte"]?.toIntOrNull()
                        ?: return@delete call.respond(status = HttpStatusCode.BadRequest, message = "Id producte no valid")
                    val idUsuari = call.principal<JWTPrincipal>()
                        ?.payload?.getClaim("idUsuari")?.asInt()
                        ?: return@delete call.respond(status = HttpStatusCode.Unauthorized, message = "unauthorized")
                    val exit = RepositoriProductesDeLaLlista.eliminaProducte(idLlista, idProducte)
                    if (exit) {
                        call.respond(status = HttpStatusCode.OK, message = "Producte eliminat de la llista")
                        val llista = RepositoriLlistaDeLaCompra.cercaLlistaPerId(idLlista)
                        val aNotificar = llista?.propietaris?.filter { it != idUsuari } ?: emptyList()
                        if (aNotificar.isNotEmpty()) {
                            GestorDeConnexions.enviaAUsuarisConcrects(
                                idsUsuaris = aNotificar,
                                esdevenimet = EsdevenimentLlista(
                                    accio = TipusAccio.PRODUCTE_ELIMINAT,
                                    idLlista = idLlista,
                                    idRecursAfectat = idProducte,
                                    producte = null
                                )
                            )
                        }
                    } else {
                        call.respond(status = HttpStatusCode.NotFound, message = "Producte no trobat a la llista")
                    }
                }
            }
            route("{idPropietari}") {
                post {
                    val idUsuari = call.principal<JWTPrincipal>()
                        ?.payload?.getClaim("idUsuari")?.asInt()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized, "unauthorized")
                    val idLlista = call.parameters["idLlista"]?.toIntOrNull()
                        ?: return@post call.respond(HttpStatusCode.BadRequest, "Id de llista no vàlid")
                    val idPropietari = call.parameters["idPropietari"]?.toIntOrNull()
                        ?: return@post call.respond(HttpStatusCode.BadRequest, "Id de propietari no vàlid")


                    val llista = RepositoriLlistaDeLaCompra.cercaLlistaPerId(idLlista)
                        ?: return@post call.respond(HttpStatusCode.NotFound, "Llista no trobada")

                    val exit = RepositoriUsuaris.afegeixComAPropietariAUnaLlista(idPropietari, idLlista)
                    if (exit) {
                        call.respond(HttpStatusCode.OK, "Propietari afegit")


                        val aNotificar = (llista.propietaris + idPropietari)
                            .distinct()
                            .filter { it != idUsuari }

                        if (aNotificar.isNotEmpty()) {
                            GestorDeConnexions.enviaAUsuarisConcrects(
                                idsUsuaris = aNotificar,
                                esdevenimet = EsdevenimentLlista(
                                    accio = TipusAccio.LLISTA_ACTUALITZADA,
                                    idLlista = idLlista,
                                    idRecursAfectat = idPropietari,
                                    producte = null
                                )
                            )
                        }
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "No s'ha pogut afegir el propietari")
                    }
                }

                delete {
                    val idUsuari = call.principal<JWTPrincipal>()
                        ?.payload?.getClaim("idUsuari")?.asInt()
                        ?: return@delete call.respond(HttpStatusCode.Unauthorized, "unauthorized")
                    val idLlista = call.parameters["idLlista"]?.toIntOrNull()
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, "Id de llista no vàlid")
                    val idPropietari = call.parameters["idPropietari"]?.toIntOrNull()
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, "Id de propietari no vàlid")


                    val llista = RepositoriLlistaDeLaCompra.cercaLlistaPerId(idLlista)
                        ?: return@delete call.respond(HttpStatusCode.NotFound, "Llista no trobada")

                    val exit = RepositoriUsuaris.eliminaComAPropietariAUnaLlista(idPropietari, idLlista)
                    if (exit) {
                        call.respond(HttpStatusCode.OK, "Propietari eliminat")


                        val aNotificar = llista.propietaris.filter { it != idUsuari }
                        if (aNotificar.isNotEmpty()) {
                            GestorDeConnexions.enviaAUsuarisConcrects(
                                idsUsuaris = aNotificar,
                                esdevenimet = EsdevenimentLlista(
                                    accio = TipusAccio.LLISTA_ACTUALITZADA,
                                    idLlista = idLlista,
                                    idRecursAfectat = idPropietari,
                                    producte = null
                                )
                            )
                        }
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Propietari no trobat")
                    }
                }
            }
        }
    }
}