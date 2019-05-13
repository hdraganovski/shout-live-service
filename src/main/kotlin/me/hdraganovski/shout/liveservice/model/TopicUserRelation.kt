package me.hdraganovski.shout.liveservice.model

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table


object TopicUserRelations: Table() {
    var id = long("id").primaryKey().autoIncrement()
    var userId = reference("user_id", Users.id, ReferenceOption.CASCADE)
    var topicId = reference("topic_id", Topics.id, ReferenceOption.CASCADE)
}

data class TopicUserRelation(
    var id: Long,
    var userId: Long,
    var topicId: Long
)