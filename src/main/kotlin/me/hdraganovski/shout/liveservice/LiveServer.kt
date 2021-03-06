package me.hdraganovski.shout.liveservice

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.WebSocketSession
import io.ktor.http.cio.websocket.close
import kotlinx.coroutines.channels.ClosedSendChannelException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class LiveServer {

    val usersCounter = AtomicInteger()

    /**
     * A concurrent map associating session IDs to user names.
     */
    val memberNames = ConcurrentHashMap<String, String>()

    /**
     * A concurrent map associating session IDs to user data
     */
    private val memberData = ConcurrentHashMap<String, MemberData>()

    /**
     * Associates a session-id to a set of websockets.
     * Since a browser is able to open several tabs and windows with the same cookies and thus the same session.
     * There might be several opened sockets for the same client.
     */
    val members = ConcurrentHashMap<String, MutableList<WebSocketSession>>()

    /**
     * Handles that a member identified with a session id and a socket joined.
     */
    suspend fun memberJoin(member: String, socket: WebSocketSession) {
        // Checks if this user is already registered in the server and gives him/her a temporal name if required.
        val name = memberNames.computeIfAbsent(member) { "user${usersCounter.incrementAndGet()}" }

        memberData.computeIfAbsent(member) { MemberData(null, true) }

        // Associates this socket to the member id.
        // Since iteration is likely to happen more frequently than adding new items,
        // we use a `CopyOnWriteArrayList`.
        // We could also control how many sockets we would allow per client here before appending it.
        // But since this is a sample we are not doing it.
        val list = members.computeIfAbsent(member) { CopyOnWriteArrayList<WebSocketSession>() }
        list.add(socket)

    }

    /**
     * Handles that a [member] with a specific [socket] left the server.
     */
    suspend fun memberLeft(member: String, socket: WebSocketSession) {
        // Removes the socket connection for this member
        val connections = members[member]
        connections?.remove(socket)

        // If no more sockets are connected for this member, let's remove it from the server
        // and notify the rest of the users about this event.
        if (connections != null && connections.isEmpty()) {
            memberData.remove(member)
            members.remove(member)
        }
    }

    suspend fun setLocation(member: String, location: Location) {
        memberData[member]?.apply {
            this.location = location
            this.global = false
        }
        sendTo(member, "INFO", "$location")
    }

    suspend fun setLocation(member: String, location: String) {
        try {
            setLocation(member, location.toLocation())
        }
        catch(e: Throwable) {
            sendTo(member, "ERROR", "Invalid location: $location")
        }
    }

    suspend fun setDistance(member: String, distance: String) {
        try {
            memberData[member]?.apply {
                this.dist = distance.trim().toDouble()
            }
        } catch (e: Throwable) {
            sendTo(member, "ERROR", "Invalid request: $distance")
        }
    }

    suspend fun dump(member: String) {
        sendTo(member, "INFO", "$memberData")
    }

    suspend fun removeLocation(member: String) {
        memberData[member]?.apply {
            this.location = null
            this.global = true
        }
    }


    /**
     * Handles sending to a [recipient] from a [sender] a [message].
     *
     * Both [recipient] and [sender] are identified by its session-id.
     */
    suspend fun sendTo(recipient: String, sender: String, message: String) {
        members[recipient]?.send(Frame.Text("[$sender] $message"))
    }

    /**
     * Handles a [message] sent from a [sender] by notifying the rest of the users.
     */
    suspend fun message(sender: String, message: String) {
        // Pre-format the message to be send, to prevent doing it for all the users or connected sockets.
        val name = memberNames[sender] ?: sender
        val formatted = "[$name] $message"

        // Sends this pre-formatted message to all the members in the server.
        broadcast(formatted)
    }

    /**
     * Sends a [message] to all the members in the server, including all the connections per member.
     */
    suspend fun broadcast(message: String) {
        members.values.forEach { socket ->
            socket.send(Frame.Text(message))
        }
    }

    /**
     * Sends a [message] coming from a [sender] to all the members in the server, including all the connections per member.
     */
    private suspend fun broadcast(sender: String, message: String) {
        val name = memberNames[sender] ?: sender
        broadcast("[$name] $message")
    }

    /**
     * Sends a [message] to a list of [this] [WebSocketSession].
     */
    suspend fun List<WebSocketSession>.send(frame: Frame) {
        forEach {
            try {
                it.send(frame.copy())
            } catch (t: Throwable) {
                try {
                    it.close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, ""))
                } catch (ignore: ClosedSendChannelException) {
                    // at some point it will get closed
                }
            }
        }
    }
}

private data class MemberData(
        var location: Location?,
        var global: Boolean,
        var dist: Double = 100.0
)

data class Location(
        var lat: Double,
        var lon: Double
)

fun String.toLocation(): Location{
    val part = trim().split("\\s".toRegex())

    if(part.size != 2) throw Throwable("Invalid format")

    return Location(
            part[0].toDouble(),
            part[1].toDouble()
    )

}