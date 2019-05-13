package me.hdraganovski.shout.liveservice.model

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table


object FirebaseTokens: Table() {
    var id = long("id").primaryKey().autoIncrement()
    var token = varchar("token", 255)
    var userId = reference("user_id", Users.id, ReferenceOption.CASCADE)
}

data class FirebaseToken(
    var id: Long,
    var token: String,
    var userId: Long
)