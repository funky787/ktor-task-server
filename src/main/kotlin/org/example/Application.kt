package org.example

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.application.call
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Task(
    val id: Int,
    val title: String,
    val done: Boolean = false
)

@Serializable
data class CreateTaskRequest(
    val title: String,
    val done: Boolean = false
)

@Serializable
data class ErrorResponse(
    val code: Int,
    val message: String
)

@Serializable
data class MessageResponse(
    val message: String
)

private val tasks = mutableListOf(
    Task(1, "Изучить Ktor"),
    Task(2, "Сделать REST API", true),
    Task(3, "Загрузить проект в GitHub")
)

private var nextId = 4

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            }
        )
    }

    install(StatusPages) {
        exception<BadRequestException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(400, "Неверный формат запроса")
            )
        }

        exception<Throwable> { call, _ ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(500, "Внутренняя ошибка сервера")
            )
        }
    }

    routing {
        get("/") {
            call.respond(
                HttpStatusCode.OK,
                MessageResponse("Ktor server работает")
            )
        }

        get("/tasks") {
            call.respond(HttpStatusCode.OK, tasks)
        }

        get("/tasks/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(400, "Некорректный id")
                )
                return@get
            }

            val task = tasks.find { it.id == id }

            if (task == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse(404, "Задача не найдена")
                )
                return@get
            }

            call.respond(HttpStatusCode.OK, task)
        }

        get("/search") {
            val query = call.request.queryParameters["query"]

            if (query.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(400, "Укажите query параметр")
                )
                return@get
            }

            val result = tasks.filter {
                it.title.contains(query, ignoreCase = true)
            }

            call.respond(HttpStatusCode.OK, result)
        }

        post("/tasks") {
            val request = call.receive<CreateTaskRequest>()

            if (request.title.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(400, "Название задачи не может быть пустым")
                )
                return@post
            }

            val task = Task(
                id = nextId++,
                title = request.title,
                done = request.done
            )

            tasks.add(task)
            call.respond(HttpStatusCode.Created, task)
        }

        delete("/tasks/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(400, "Некорректный id")
                )
                return@delete
            }

            val task = tasks.find { it.id == id }

            if (task == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse(404, "Задача не найдена")
                )
                return@delete
            }

            tasks.remove(task)
            call.respond(
                HttpStatusCode.OK,
                MessageResponse("Задача удалена")
            )
        }
    }
}
