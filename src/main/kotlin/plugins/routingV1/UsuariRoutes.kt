package com.example.plugins.routingV1

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.model.request.PeticioActualitzacioUsuari
import com.example.model.request.PeticioRegistreUsuari
import com.example.model.websockets.TipusAccio
import com.example.utils.GestorDeConnexions
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import model.Usuari
import model.toCampActualitzable
import repositoris.RepositoriUsuaris
import java.util.Date
import kotlin.collections.hashMapOf

fun Route.rutesUsuaris() {
    val config = application.environment.config
    val secret = config.property("jwt.secret").getString()
    val issuer = config.property("jwt.issuer").getString()
    val audience = config.property("jwt.audience").getString()
    //AQUI VA EL LOGIN
    post("login") {
        val user = call.receive<Usuari>()
        val usuariTrobat = RepositoriUsuaris.obtenTots()
            .find { it.nomUsuari == user.nomUsuari && it.password == user.password }

        if (usuariTrobat != null) {
            val token = JWT.create()
                .withAudience(audience)
                .withIssuer(issuer)
                .withClaim("idUsuari", usuariTrobat.id)  // <- pon idUsuari, no username
                .withExpiresAt(Date(System.currentTimeMillis() + 604800000))
                .sign(Algorithm.HMAC256(secret))
            call.respond(hashMapOf("token" to token))
        } else {
            call.respond(status = HttpStatusCode.NotFound, "No existeix l'usuari")
        }
    }


        get("usuaris") {
            println("en endpoint usuaris")
            call.respond(RepositoriUsuaris.obtenTots())

        }

        post("registre") {
            val parametres = call.receive<PeticioRegistreUsuari>()

            RepositoriUsuaris.creaUsuari(
                parametres.nomUsuari,
                parametres.password,
                parametres.alias
            )?.let {
                call.respond(
                    status = HttpStatusCode.Created, message = mapOf(
                        "username" to it.nomUsuari,
                        "alias" to it.alias
                    )
                )
            } ?: call.respond(status = HttpStatusCode.BadRequest, message = "No s'ha pogut crear l'usuari")
        }

        authenticate("auth-jwt") {
            route("me") {
                get {
                    val idUsuari = call.principal<JWTPrincipal>()
                        ?.payload?.getClaim("idUsuari")?.asInt()
                        ?: return@get call.respond(status = HttpStatusCode.Unauthorized, message = "unauthorized")

                    val usuari = RepositoriUsuaris.cercaUsuariPerID(idUsuari)
                    if (usuari != null) {
                        call.respond(usuari)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
                delete {
                    val idUsuari = call.principal<JWTPrincipal>()
                        ?.payload?.getClaim("idUsuari")?.asInt()
                        ?: return@delete call.respond(status = HttpStatusCode.Unauthorized, message = "unauthorized")

                    val usuari = RepositoriUsuaris.eliminarUsuari(idUsuari)
                    if (usuari) {
                        call.respond(status = HttpStatusCode.OK, "Usuari eliminat correctament")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "usuari no trobat")
                    }
                }
                patch {
                    val idUsuari = call.principal<JWTPrincipal>()
                        ?.payload?.getClaim("idUsuari")?.asInt()
                        ?: return@patch call.respond(status = HttpStatusCode.Unauthorized, message = "unauthorized")

                    val parametres = call.receive<PeticioActualitzacioUsuari>()

                    val exit = RepositoriUsuaris.actualitzaUsuari(
                        idUsuari,
                        _nom = parametres.nomUsuari.toCampActualitzable(),
                        _password = parametres.motDePas.toCampActualitzable(),
                        _alias = parametres.alias.toCampActualitzable(),
                    )
                    if (exit) {
                        call.respond(status = HttpStatusCode.OK, "Usuari actualitzat correctament")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "usuari no trobat")
                    }
                }
            }

            route("me/amics") {
                get {
                    val idUsuari = call.principal<JWTPrincipal>()
                        ?.payload?.getClaim("idUsuari")?.asInt()
                        ?: return@get call.respond(status = HttpStatusCode.Unauthorized, message = "unauthorized")

                    val amics = RepositoriUsuaris.obtenAmics(idUsuari)
                    call.respond(amics)
                }
                route("{idAmic}") {
                    post {
                        val idUsuari = call.principal<JWTPrincipal>()
                            ?.payload?.getClaim("idUsuari")?.asInt()
                            ?: return@post call.respond(status = HttpStatusCode.Unauthorized, message = "unauthorized")
                        val idAmic = call.parameters["idAmic"]?.toIntOrNull()
                            ?: return@post call.respond(status = HttpStatusCode.BadRequest, message = "el id amic no es valid")
                        val exit = RepositoriUsuaris.afegeixAmic(idUsuari, idAmic)
                        if (exit) {
                            call.respond(status = HttpStatusCode.OK, "Amic afegit")
                            GestorDeConnexions.enviaAUsuarisConcrects(
                                listOf(idAmic),
                                esdevenimet = EsdevenimentLlista(
                                    accio = TipusAccio.NOTIFICACIO_AMISTAT_NOVA,
                                    idLlista = null,
                                    idRecursAfectat = idUsuari,
                                    producte = null
                                )
                            )
                        } else {
                            call.respond(status = HttpStatusCode.BadRequest, "mal")
                        }
                    }
                    /*delete {
                        val idUsuari = call.principal<JWTPrincipal>()
                            ?.payload?.getClaim("idUsuari")?.asInt()
                            ?: return@delete call.respond(status = HttpStatusCode.Unauthorized, message = "unauthorized")
                        val idAmic = call.parameters["idAmic"]?.toIntOrNull()
                            ?: return@delete call.respond(status = HttpStatusCode.BadRequest, message = "el id amic no es valid")
                       // val exit = RepositoriUsuaris.eliminaAmic(idUsuari, idAmic)
                        if (exit) {
                            call.respond(status = HttpStatusCode.OK, "Amic eliminat")
                        } else {
                            call.respond(status = HttpStatusCode.NotFound, "Amic no trobat")
                        }
                    }*/
                }
            }
        }
    }

