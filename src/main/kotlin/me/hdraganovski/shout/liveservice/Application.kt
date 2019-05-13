package me.hdraganovski.shout.liveservice

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.http.*
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.close
import io.ktor.http.cio.websocket.readText
import io.ktor.jackson.jackson
import io.ktor.request.path
import io.ktor.request.receive
import io.ktor.request.receiveText
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.sessions.*
import io.ktor.util.generateNonce
import io.ktor.websocket.webSocket
import kotlinx.coroutines.channels.consumeEach
import me.dragon.shout.liveservice.LiveServer
import me.hdraganovski.shout.liveservice.model.Topic
import me.hdraganovski.shout.liveservice.model.User
import me.hdraganovski.shout.liveservice.service.DatabaseFactory
import me.hdraganovski.shout.liveservice.service.TopicService
import me.hdraganovski.shout.liveservice.service.UserService
import org.slf4j.event.Level
import java.time.Duration

fun main(args: Array<String>) {

    val port = Integer.valueOf(System.getenv("PORT"))
    embeddedServer(Netty, port) {
        liveServiceModule()
    }.start(wait = true)
}

val server = LiveServer()


fun Application.liveServiceModule(testing: Boolean = false) {

    DatabaseFactory.init()

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.Authorization)
        header("MyCustomHeader")
        allowCredentials = true
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }

    install(io.ktor.websocket.WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    install(Authentication) {
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    install(Sessions) {
        cookie<LiveServiceSession>("Session")
    }

    // This adds an interceptor that will create a specific session in each request if no session is available already.
    intercept(ApplicationCallPipeline.Features) {
        if (call.sessions.get<LiveServiceSession>() == null) {
            call.sessions.set(LiveServiceSession(generateNonce()))
        }
    }

    routing {
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        webSocket("/myws/echo") {
            send(Frame.Text("Hi from server"))
            while (true) {
                val frame = incoming.receive()
                if (frame is Frame.Text) {
                    send(Frame.Text("Client said: " + frame.readText()))
                }
            }
        }

        webSocket("/ws") {
            // First of all we get the session.
            val session = call.sessions.get<LiveServiceSession>()

            // We check that we actually have a session. We should always have one,
            // since we have defined an interceptor before to set one.
            if (session == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No session"))
                return@webSocket
            }

            // We notify that a member joined by calling the server handler [memberJoin]
            // This allows to associate the session id to a specific WebSocket connection.
            server.memberJoin(session.id, this)

            try {
                // We starts receiving messages (frames).
                // Since this is a coroutine. This coroutine is suspended until receiving frames.
                // Once the connection is closed, this consumeEach will finish and the code will continue.
                incoming.consumeEach { frame ->
                    // Frames can be [Text], [Binary], [Ping], [Pong], [Close].
                    // We are only interested in textual messages, so we filter it.
                    when (frame) {
                        is Frame.Text -> {
                            // Now it is time to process the text sent from the user.
                            // At this point we have context about this connection, the session, the text and the server.
                            // So we have everything we need.
                            receivedMessage(session.id, frame.readText())
                        }
                        is Frame.Ping -> {

                        }
                    }

                }
            } finally {
                // Either if there was an error, of it the connection was closed gracefully.
                // We notify the server that the member left.
                server.memberLeft(session.id, this)
            }
        }

        post("/broadcast") {
            try {
                val body = call.receiveText()
                server.broadcast(body)
                call.respond(HttpStatusCode.OK, "OK")
            } finally {
                call.respond(HttpStatusCode.InternalServerError, "Error")
            }
        }

        get("/json/jackson") {
            call.respond(mapOf("hello" to "world"))
        }

        route("/user") {
            get("/") {
                call.respond(UserService.getAll())
            }
            post("/") {
                val user: User = call.receive()
                call.respond(UserService.addUser(user))
            }
        }

        route("/topic") {
            get("/") {
                call.respond(TopicService.getAll())
            }

            post("/") {
                val topic: Topic = call.receive()
                call.respond(TopicService.addTopic(topic))
            }
        }

        route("/token") {
            post("/") {

            }
        }
    }
}

data class LiveServiceSession(val id: String)

private suspend fun receivedMessage(id: String, command: String) {
    // We are going to handle commands (text starting with '/') and normal messages
    server.message(id, command)
}
