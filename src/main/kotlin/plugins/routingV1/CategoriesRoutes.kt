package com.example.plugins.routingV1
import com.example.model.request.PeticioCategoria
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
import repositoris.RepositoriCategories



fun Route.rutesCategories() {
    authenticate("auth-jwt") {
        route("categories") {
            get {
                call.respond(RepositoriCategories.obtenTotes())
            }
            post {
                val parametres = call.receive<PeticioCategoria>()
                val categoriaCreada = RepositoriCategories.creaCategoria(parametres.nomCategoria)
                if (categoriaCreada != null) {
                    call.respond(status = HttpStatusCode.Created, message = categoriaCreada)
                } else {
                    call.respond(status = HttpStatusCode.BadRequest, message = "No s'ha pogut crear la categoria")
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
            }
        }
    }
}