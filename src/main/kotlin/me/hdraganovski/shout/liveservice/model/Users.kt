package me.hdraganovski.shout.liveservice.model

import org.jetbrains.exposed.sql.Table

object Users: Table() {
    var id = long("id").primaryKey()
}

data class User(
    var id: Long
)