package com.example

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    val jsonResponse = """{
        "id": 1,
        "task": "Pay waterbill",
        "description": "Pay water bill today",
    }"""

    embeddedServer(Netty, 80) {
        install(Routing) {
            get("/") {
                call.respondText("Hello World!", ContentType.Text.Plain)
            }
            get("/todo") {
                call.respondText(jsonResponse, ContentType.Application.Json)
            }
        }
    }.start(wait = true)
}