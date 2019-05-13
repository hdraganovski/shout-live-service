package me.hdraganovski.shout.liveservice.model

import org.jetbrains.exposed.sql.Table

object Topics: Table() {
    var id = long("id").primaryKey().autoIncrement()
    var title = varchar("title", 255)
}

data class Topic(
    var id: Long?,
    var title: String
)

