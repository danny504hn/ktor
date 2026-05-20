package com.example.plugins.routingV1

import com.example.model.request.*
import com.example.model.websockets.TipusAccio
import com.example.repositoris.RepositoriLlistaDeLaCompra
import com.example.repositoris.RepositoriProductesDeLaLlista
import com.example.utils.GestorDeConnexions
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
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
            }.describe {
                summary = "Llista totes les llistes de l'usuari"
                description = "Retorna totes les llistes de la compra on l'usuari autenticat és propietari."
                tag("Llistes")
                responses {
                    HttpStatusCode.OK { description = "Llista de llistes retornada correctament" }
                    HttpStatusCode.Unauthorized { description = "Token JWT invàlid o absent" }
                }
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
            }.describe {
                summary = "Crea una nova llista"
                description = "Crea una nova llista de la compra. L'usuari autenticat és afegit automàticament com a propietari."
                tag("Llistes")
                requestBody {
                    description = "Dades de la nova llista"
                    schema = jsonSchema<PeticioLlista>()
                }
                responses {
                    HttpStatusCode.Created { description = "Llista creada correctament" }
                    HttpStatusCode.BadRequest { description = "Falta el nom de la llista o no s'ha pogut crear" }
                    HttpStatusCode.Unauthorized { description = "Token JWT invàlid o absent" }
                }
            }

            patch("{idLlista}") {
                val idLlista = call.parameters["idLlista"]?.toIntOrNull()
                    ?: return@patch call.respond(status = HttpStatusCode.BadRequest, message = "Id no valid")
                val idUsuari = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("idUsuari")?.asInt()
                    ?: return@patch call.respond(status = HttpStatusCode.Unauthorized, message = "unauthorized")
                val parametres = call.receive<PeticioLlista>()
                val llista = RepositoriLlistaDeLaCompra.cercaLlistaPerId(idLlista)
                RepositoriLlistaDeLaCompra.actualitzaLlista(
                    idLlista,
                    nouNomLlista = parametres.nomLlista.toCampActualitzable(),
                    idsPropietaris = parametres.idsPropietaris.toCampActualitzable()
                )
                call.respond(status = HttpStatusCode.OK, message = "Llista actualitzada")
                val aNotificar = llista?.propietaris?.filter { it != idUsuari } ?: emptyList()
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
            }.describe {
                summary = "Modifica una llista"
                description = "Actualitza el nom i/o els propietaris d'una llista. Notifica via WebSocket als altres propietaris."
                tag("Llistes")
                parameters {
                    path("idLlista") {
                        description = "Identificador únic de la llista a modificar"
                        required = true
                    }
                }
                requestBody {
                    description = "Dades a actualitzar de la llista (nom i/o propietaris)"
                    schema = jsonSchema<PeticioLlista>()
                }
                responses {
                    HttpStatusCode.OK { description = "Llista actualitzada correctament" }
                    HttpStatusCode.BadRequest { description = "Id de llista no vàlid" }
                    HttpStatusCode.Unauthorized { description = "Token JWT invàlid o absent" }
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
            }.describe {
                summary = "Elimina una llista"
                description = "Elimina una llista de la compra i tots els seus productes associats."
                tag("Llistes")
                parameters {
                    path("idLlista") {
                        description = "Identificador únic de la llista a eliminar"
                        required = true
                    }
                }
                responses {
                    HttpStatusCode.OK { description = "Llista eliminada correctament" }
                    HttpStatusCode.NotFound { description = "No s'ha trobat cap llista amb aquest id" }
                    HttpStatusCode.BadRequest { description = "Id de llista no vàlid" }
                }
            }

            // Productes de la llista
            route("{idLlista}/productes") {
                get {
                    val idLlista = call.parameters["idLlista"]?.toIntOrNull()
                        ?: return@get call.respond(status = HttpStatusCode.BadRequest, message = "Id no valid")
                    call.respond(RepositoriProductesDeLaLlista.cercaProductesPerLlista(idLlista))
                }.describe {
                    summary = "Llista els productes d'una llista"
                    description = "Retorna tots els productes associats a una llista de la compra."
                    tag("Productes de la llista")
                    parameters {
                        path("idLlista") {
                            description = "Identificador únic de la llista"
                            required = true
                        }
                    }
                    responses {
                        HttpStatusCode.OK { description = "Llista de productes retornada correctament" }
                        HttpStatusCode.BadRequest { description = "Id de llista no vàlid" }
                    }
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
                }.describe {
                    summary = "Afegeix un producte a la llista"
                    description = "Afegeix un producte existent a una llista de la compra. Notifica via WebSocket als altres propietaris."
                    tag("Productes de la llista")
                    parameters {
                        path("idLlista") {
                            description = "Identificador únic de la llista"
                            required = true
                        }
                    }
                    requestBody {
                        description = "Dades del producte a afegir. idProducte i nomProducte són obligatoris."
                        schema = jsonSchema<PeticioProducteDeLaLlista>()
                    }
                    responses {
                        HttpStatusCode.Created { description = "Producte afegit correctament a la llista" }
                        HttpStatusCode.BadRequest { description = "Falten dades obligatòries o id de llista no vàlid" }
                        HttpStatusCode.Unauthorized { description = "Token JWT invàlid o absent" }
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
                }.describe {
                    summary = "Modifica un producte de la llista"
                    description = "Actualitza les dades d'un producte dins d'una llista. Notifica via WebSocket als altres propietaris."
                    tag("Productes de la llista")
                    parameters {
                        path("idLlista") {
                            description = "Identificador únic de la llista"
                            required = true
                        }
                        path("idProducte") {
                            description = "Identificador únic del producte"
                            required = true
                        }
                    }
                    requestBody {
                        description = "Dades a actualitzar del producte"
                        schema = jsonSchema<PeticioProducteDeLaLlista>()
                    }
                    responses {
                        HttpStatusCode.OK { description = "Producte actualitzat correctament" }
                        HttpStatusCode.NotFound { description = "Producte no trobat a la llista" }
                        HttpStatusCode.BadRequest { description = "Id de llista o producte no vàlid" }
                        HttpStatusCode.Unauthorized { description = "Token JWT invàlid o absent" }
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
                }.describe {
                    summary = "Elimina un producte de la llista"
                    description = "Elimina un producte d'una llista de la compra. Notifica via WebSocket als altres propietaris."
                    tag("Productes de la llista")
                    parameters {
                        path("idLlista") {
                            description = "Identificador únic de la llista"
                            required = true
                        }
                        path("idProducte") {
                            description = "Identificador únic del producte a eliminar"
                            required = true
                        }
                    }
                    responses {
                        HttpStatusCode.OK { description = "Producte eliminat correctament de la llista" }
                        HttpStatusCode.NotFound { description = "Producte no trobat a la llista" }
                        HttpStatusCode.BadRequest { description = "Id de llista o producte no vàlid" }
                        HttpStatusCode.Unauthorized { description = "Token JWT invàlid o absent" }
                    }
                }
            }

            // Propietaris
            route("{idLlista}/{idPropietari}") {
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
                        val aNotificar = (llista.propietaris + idPropietari).distinct().filter { it != idUsuari }
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
                }.describe {
                    summary = "Afegeix un propietari a una llista"
                    description = "Afegeix un usuari com a propietari d'una llista. Notifica via WebSocket als altres propietaris i al nou propietari."
                    tag("Propietaris")
                    parameters {
                        path("idLlista") {
                            description = "Identificador únic de la llista"
                            required = true
                        }
                        path("idPropietari") {
                            description = "Identificador de l'usuari a afegir com a propietari"
                            required = true
                        }
                    }
                    responses {
                        HttpStatusCode.OK { description = "Propietari afegit correctament" }
                        HttpStatusCode.NotFound { description = "Llista no trobada" }
                        HttpStatusCode.BadRequest { description = "Id no vàlid o no s'ha pogut afegir" }
                        HttpStatusCode.Unauthorized { description = "Token JWT invàlid o absent" }
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
                }.describe {
                    summary = "Elimina un propietari d'una llista"
                    description = "Elimina un usuari com a propietari d'una llista. Notifica via WebSocket als propietaris restants."
                    tag("Propietaris")
                    parameters {
                        path("idLlista") {
                            description = "Identificador únic de la llista"
                            required = true
                        }
                        path("idPropietari") {
                            description = "Identificador de l'usuari a eliminar com a propietari"
                            required = true
                        }
                    }
                    responses {
                        HttpStatusCode.OK { description = "Propietari eliminat correctament" }
                        HttpStatusCode.NotFound { description = "Llista o propietari no trobat" }
                        HttpStatusCode.BadRequest { description = "Id no vàlid" }
                        HttpStatusCode.Unauthorized { description = "Token JWT invàlid o absent" }
                    }
                }
            }
        }
    }
}