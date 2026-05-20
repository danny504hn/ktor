package com.example.plugins.routingV1

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.model.request.PeticioActualitzacioUsuari
import com.example.model.request.PeticioRegistreUsuari
import com.example.model.websockets.TipusAccio
import com.example.utils.GestorDeConnexions
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import model.Usuari
import model.toCampActualitzable
import repositoris.RepositoriUsuaris
import java.util.Date

fun Route.rutesUsuaris() {
    val config = application.environment.config
    val secret = config.property("jwt.secret").getString()
    val issuer = config.property("jwt.issuer").getString()
    val audience = config.property("jwt.audience").getString()

    post("login") {
        val user = call.receive<Usuari>()
        val usuariTrobat = RepositoriUsuaris.obtenTots()
            .find { it.nomUsuari == user.nomUsuari && it.password == user.password }
        if (usuariTrobat != null) {
            val token = JWT.create()
                .withAudience(audience)
                .withIssuer(issuer)
                .withClaim("idUsuari", usuariTrobat.id)
                .withExpiresAt(Date(System.currentTimeMillis() + 604800000))
                .sign(Algorithm.HMAC256(secret))
            call.respond(hashMapOf("token" to token))
        } else {
            call.respond(status = HttpStatusCode.NotFound, "No existeix l'usuari")
        }
    }.describe {
        summary = "Inicia sessió"
        description = "Rep nomUsuari i password en format JSON i retorna un token JWT. Copia el token i clica 'Authorize' a la part superior per usar els endpoints protegits."
        tag("Autenticació")
        requestBody {
            description = "Credencials de l'usuari"
            schema = jsonSchema<Usuari>()
        }
        responses {
            HttpStatusCode.OK { description = "Login correcte. Retorna el token JWT." }
            HttpStatusCode.NotFound { description = "Usuari no trobat o credencials incorrectes" }
        }
    }

    get("usuaris") {
        call.respond(RepositoriUsuaris.obtenTots())
    }.describe {
        summary = "Llista tots els usuaris"
        description = "Retorna la llista completa d'usuaris registrats."
        tag("Usuaris")
        responses {
            HttpStatusCode.OK { description = "Llista d'usuaris retornada correctament" }
        }
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
    }.describe {
        summary = "Registra un nou usuari"
        description = "Crea un nou usuari amb nom d'usuari, contrasenya i alias opcional."
        tag("Autenticació")
        requestBody {
            description = "Dades del nou usuari"
            schema = jsonSchema<PeticioRegistreUsuari>()
        }
        responses {
            HttpStatusCode.Created { description = "Usuari creat correctament. Retorna username i alias." }
            HttpStatusCode.BadRequest { description = "No s'ha pogut crear l'usuari" }
        }
    }

    authenticate("auth-jwt") {
        route("me") {
            get {
                val idUsuari = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("idUsuari")?.asInt()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, "unauthorized")
                val usuari = RepositoriUsuaris.cercaUsuariPerID(idUsuari)
                if (usuari != null) call.respond(usuari)
                else call.respond(HttpStatusCode.NotFound)
            }.describe {
                summary = "Obté el perfil de l'usuari autenticat"
                description = "Retorna les dades de l'usuari identificat pel token JWT."
                tag("Usuaris")
                responses {
                    HttpStatusCode.OK { description = "Dades de l'usuari retornades correctament" }
                    HttpStatusCode.NotFound { description = "Usuari no trobat" }
                    HttpStatusCode.Unauthorized { description = "Token JWT invàlid o absent" }
                }
            }

            delete {
                val idUsuari = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("idUsuari")?.asInt()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, "unauthorized")
                val usuari = RepositoriUsuaris.eliminarUsuari(idUsuari)
                if (usuari) call.respond(HttpStatusCode.OK, "Usuari eliminat correctament")
                else call.respond(HttpStatusCode.NotFound, "usuari no trobat")
            }.describe {
                summary = "Elimina el compte de l'usuari autenticat"
                description = "Elimina permanentment el compte de l'usuari identificat pel token JWT."
                tag("Usuaris")
                responses {
                    HttpStatusCode.OK { description = "Usuari eliminat correctament" }
                    HttpStatusCode.NotFound { description = "Usuari no trobat" }
                    HttpStatusCode.Unauthorized { description = "Token JWT invàlid o absent" }
                }
            }

            patch {
                val idUsuari = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("idUsuari")?.asInt()
                    ?: return@patch call.respond(HttpStatusCode.Unauthorized, "unauthorized")
                val parametres = call.receive<PeticioActualitzacioUsuari>()
                val exit = RepositoriUsuaris.actualitzaUsuari(
                    idUsuari,
                    _nom = parametres.nomUsuari.toCampActualitzable(),
                    _password = parametres.motDePas.toCampActualitzable(),
                    _alias = parametres.alias.toCampActualitzable(),
                )
                if (exit) call.respond(HttpStatusCode.OK, "Usuari actualitzat correctament")
                else call.respond(HttpStatusCode.NotFound, "usuari no trobat")
            }.describe {
                summary = "Modifica el perfil de l'usuari autenticat"
                description = "Actualitza el nom, contrasenya i/o alias de l'usuari identificat pel token JWT."
                tag("Usuaris")
                requestBody {
                    description = "Dades a actualitzar (tots els camps són opcionals)"
                    schema = jsonSchema<PeticioActualitzacioUsuari>()
                }
                responses {
                    HttpStatusCode.OK { description = "Usuari actualitzat correctament" }
                    HttpStatusCode.NotFound { description = "Usuari no trobat" }
                    HttpStatusCode.Unauthorized { description = "Token JWT invàlid o absent" }
                }
            }
        }

        route("me/amics") {
            get {
                val idUsuari = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("idUsuari")?.asInt()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, "unauthorized")
                call.respond(RepositoriUsuaris.obtenAmics(idUsuari))
            }.describe {
                summary = "Llista els amics de l'usuari autenticat"
                description = "Retorna la llista d'usuaris que són amics de l'usuari identificat pel token JWT."
                tag("Amics")
                responses {
                    HttpStatusCode.OK { description = "Llista d'amics retornada correctament" }
                    HttpStatusCode.Unauthorized { description = "Token JWT invàlid o absent" }
                }
            }

            route("{idAmic}") {
                post {
                    val idUsuari = call.principal<JWTPrincipal>()
                        ?.payload?.getClaim("idUsuari")?.asInt()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized, "unauthorized")
                    val idAmic = call.parameters["idAmic"]?.toIntOrNull()
                        ?: return@post call.respond(HttpStatusCode.BadRequest, "el id amic no es valid")
                    val exit = RepositoriUsuaris.afegeixAmic(idUsuari, idAmic)
                    if (exit) {
                        call.respond(HttpStatusCode.OK, "Amic afegit")
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
                        call.respond(HttpStatusCode.BadRequest, "mal")
                    }
                }.describe {
                    summary = "Afegeix un amic"
                    description = "Afegeix un usuari com a amic. Notifica via WebSocket a l'amic afegit."
                    tag("Amics")
                    parameters {
                        path("idAmic") {
                            description = "Identificador de l'usuari a afegir com a amic"
                            required = true
                        }
                    }
                    responses {
                        HttpStatusCode.OK { description = "Amic afegit correctament" }
                        HttpStatusCode.BadRequest { description = "Id d'amic no vàlid o no s'ha pogut afegir" }
                        HttpStatusCode.Unauthorized { description = "Token JWT invàlid o absent" }
                    }
                }

                delete {
                    val idUsuari = call.principal<JWTPrincipal>()
                        ?.payload?.getClaim("idUsuari")?.asInt()
                        ?: return@delete call.respond(HttpStatusCode.Unauthorized, "unauthorized")
                    val idAmic = call.parameters["idAmic"]?.toIntOrNull()
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, "el id amic no es valid")
                    val exit = RepositoriUsuaris.eliminaAmic(idUsuari, idAmic)
                    if (exit) {
                        call.respond(HttpStatusCode.OK, "Amic eliminat")
                        GestorDeConnexions.enviaAUsuarisConcrects(
                            idsUsuaris = listOf(idAmic),
                            esdevenimet = EsdevenimentLlista(
                                accio = TipusAccio.NOTIFICACIO_AMISTAT_ELIMINADA,
                                idLlista = null,
                                idRecursAfectat = idUsuari,
                                producte = null
                            )
                        )
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Amic no trobat")
                    }
                }.describe {
                    summary = "Elimina un amic"
                    description = "Elimina un usuari de la llista d'amics. Notifica via WebSocket a l'amic eliminat."
                    tag("Amics")
                    parameters {
                        path("idAmic") {
                            description = "Identificador de l'usuari a eliminar com a amic"
                            required = true
                        }
                    }
                    responses {
                        HttpStatusCode.OK { description = "Amic eliminat correctament" }
                        HttpStatusCode.NotFound { description = "Amic no trobat" }
                        HttpStatusCode.BadRequest { description = "Id d'amic no vàlid" }
                        HttpStatusCode.Unauthorized { description = "Token JWT invàlid o absent" }
                    }
                }
            }
        }
    }
}