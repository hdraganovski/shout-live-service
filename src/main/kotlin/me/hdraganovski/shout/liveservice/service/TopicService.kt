package me.hdraganovski.shout.liveservice.service

import me.hdraganovski.shout.liveservice.model.Topic
import me.hdraganovski.shout.liveservice.model.Topics
import me.hdraganovski.shout.liveservice.service.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll

object TopicService {

    suspend fun getTopic(id: Long): Topic? = dbQuery {
        Topics.select {
            Topics.id eq id
        }.mapNotNull {
            toTopic(it)
        }.singleOrNull()
    }

    suspend fun addTopic(topic: Topic): Topic {
        var key: Long? = null
        dbQuery {
            key = (Topics.insert {
                it[title] = topic.title
            } get Topics.id)
        }
        return getTopic(key!!)!!
    }

    suspend fun getAll(): List<Topic> = dbQuery {
        Topics.selectAll().mapNotNull {
            toTopic(it)
        }
    }

    fun toTopic(row: ResultRow): Topic {
        return Topic(
            id = row[Topics.id],
            title = row[Topics.title]
        )
    }
}