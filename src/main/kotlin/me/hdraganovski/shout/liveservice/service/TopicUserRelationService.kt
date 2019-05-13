package me.hdraganovski.shout.liveservice.service

import me.hdraganovski.shout.liveservice.service.DatabaseFactory.dbQuery
import me.hdraganovski.shout.liveservice.service.TopicService.toTopic
import me.hdraganovski.shout.liveservice.service.UserService.toUser
import me.hdraganovski.shout.liveservice.model.*
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select

object TopicUserRelationService {

    suspend fun get(key: Long): TopicUserRelation? = dbQuery {
        TopicUserRelations.select {
            Topics.id eq key
        }.mapNotNull {
            toTopicUserRelation(it)
        }.singleOrNull()
    }

    suspend fun addRelation(relation: TopicUserRelation): TopicUserRelation {
        var key: Long? = null
        dbQuery {
            key = TopicUserRelations.insert {
                it[userId] = relation.userId
                it[topicId] = relation.topicId
            } get TopicUserRelations.id
        }
        return get(key!!)!!
    }

    suspend fun getUsersForTopicID(id: Long): List<User> = dbQuery {
        ((Users innerJoin TopicUserRelations).slice(Users.id, TopicUserRelations.topicId).select {
            TopicUserRelations.topicId eq id
        }.mapNotNull {
            toUser(it)
        })
    }

    suspend fun getTopicsForUserID(id: Long): List<Topic> = dbQuery{
        ((Topics innerJoin TopicUserRelations).slice(Topics.id, Topics.title, TopicUserRelations.topicId).select {
            TopicUserRelations.userId eq id
        }.mapNotNull {
            toTopic(it)
        })
    }

    suspend fun getUsersForTopicTitle(title: String): List<User> = dbQuery {
        (Users innerJoin TopicUserRelations innerJoin Topics)
            .slice(Users.id, Topics.title)
            .select {
                Topics.title eq title
            }
            .mapNotNull {
                UserService.toUser(it)
            }
    }

    fun toTopicUserRelation(row: ResultRow): TopicUserRelation {
        return TopicUserRelation(
            id = row[TopicUserRelations.id],
            userId = row[TopicUserRelations.userId],
            topicId = row[TopicUserRelations.topicId]
        )
    }
}