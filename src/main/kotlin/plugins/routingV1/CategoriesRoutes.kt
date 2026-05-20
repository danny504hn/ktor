package com.example.plugins.routingV1
import com.example.model.request.PeticioCategoria
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import io.ktor.openapi.jsonSchema
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.patch
import io.ktor.server.routing.path
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import repositoris.RepositoriCategories

fun Route.rutesCategories() {
    authenticate("auth-jwt") {
        route("categories") {
            get {
                call.respond(RepositoriCategories.obtenTotes())
            }.describe {
                summary = "Llista totes les categories"
                description = "Retorna la llista completa de categories disponibles."
                tag("Categories")
                responses {
                    HttpStatusCode.OK { description = "Llista de categories retornada correctament" }
                    HttpStatusCode.Unauthorized { description = "Token JWT invàlid o absent" }
                }
            }

            post {
                val parametres = call.receive<PeticioCategoria>()
                val categoriaCreada = RepositoriCategories.creaCategoria(parametres.nomCategoria)
                if (categoriaCreada != null) {
                    call.respond(status = HttpStatusCode.Created, message = categoriaCreada)
                } else {
                    call.respond(status = HttpStatusCode.BadRequest, message = "No s'ha pogut crear la categoria")
                }
            }.describe {
                summary = "Crea una nova categoria"
                description = "Crea una nova categoria amb el nom indicat al cos de la petició."
                tag("Categories")
                requestBody {
                    description = "Dades de la nova categoria"
                    schema = jsonSchema<PeticioCategoria>()
                }
                responses {
                    HttpStatusCode.Created { description = "Categoria creada correctament" }
                    HttpStatusCode.BadRequest { description = "No s'ha pogut crear la categoria" }
                    HttpStatusCode.Unauthorized { description = "Token JWT invàlid o absent" }
                }
            }

            patch("{idCategoria}") {
                val idCategoria = call.parameters["idCategoria"]?.toIntOrNull()
                    ?: return@patch call.respond(status = HttpStatusCode.BadRequest, message = "Id no valid")
                val parametres = call.receive<PeticioCategoria>()
                val exit = RepositoriCategories.actualitzaNomCategoria(idCategoria, parametres.nomCategoria)
                if (exit) {
                    call.respond(status = HttpStatusCode.OK, message = "Categoria actualitzada")
                } else {
                    call.respond(status = HttpStatusCode.NotFound, message = "Categoria no trobada")
                }
            }.describe {
                summary = "Modifica el nom d'una categoria"
                description = "Actualitza el nom de la categoria identificada per idCategoria."
                tag("Categories")
                parameters {
                    path("idCategoria") {
                        description = "Identificador únic de la categoria a modificar"
                        required = true
                    }
                }
                requestBody {
                    description = "Dades actualitzades de la categoria"
                    schema = jsonSchema<PeticioCategoria>()
                }
                responses {
                    HttpStatusCode.OK { description = "Categoria actualitzada correctament" }
                    HttpStatusCode.NotFound { description = "Categoria no trobada" }
                    HttpStatusCode.BadRequest { description = "Id no vàlid" }
                    HttpStatusCode.Unauthorized { description = "Token JWT invàlid o absent" }
                }
            }

            delete("{idCategoria}") {
                val idCategoria = call.parameters["idCategoria"]?.toIntOrNull()
                    ?: return@delete call.respond(status = HttpStatusCode.BadRequest, message = "Id no valid")
                val exit = RepositoriCategories.eliminaCategoria(idCategoria)
                if (exit) {
                    call.respond(status = HttpStatusCode.OK, message = "Categoria eliminada")
                } else {
                    call.respond(status = HttpStatusCode.NotFound, message = "Categoria no trobada o te productes associats")
                }
            }.describe {
                summary = "Elimina una categoria"
                description = "Elimina la categoria identificada per idCategoria."
                tag("Categories")
                parameters {
                    path("idCategoria") {
                        description = "Identificador únic de la categoria a eliminar"
                        required = true
                    }
                }
                responses {
                    HttpStatusCode.OK { description = "Categoria eliminada correctament" }
                    HttpStatusCode.NotFound { description = "Categoria no trobada o té productes associats" }
                    HttpStatusCode.BadRequest { description = "Id no vàlid" }
                    HttpStatusCode.Unauthorized { description = "Token JWT invàlid o absent" }
                }
            }
        }
    }
}