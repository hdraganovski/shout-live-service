package me.hdraganovski.shout.liveservice.service

import me.hdraganovski.shout.liveservice.model.FirebaseToken
import me.hdraganovski.shout.liveservice.model.FirebaseTokens
import me.hdraganovski.shout.liveservice.model.User
import me.hdraganovski.shout.liveservice.service.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select

object FirebaseTokenService {
    suspend fun getToken(id: Long): FirebaseToken? {
        return dbQuery {
            FirebaseTokens.select {
                FirebaseTokens.id eq id
            }.mapNotNull {
                toFirebaseToken(it)
            }.singleOrNull()
        }
    }

    suspend fun addToken(firebaseToken: FirebaseToken): FirebaseToken {
        var key: Long? = null
        dbQuery {
            key = FirebaseTokens.insert {
                it[userId] = firebaseToken.id
                it[token] = firebaseToken.token
            } get FirebaseTokens.id
        }
        return getToken(key!!)!!
    }

    suspend fun getTokensForUser(user: User): List<FirebaseToken> = dbQuery {
        FirebaseTokens.select {
                FirebaseTokens.userId eq user.id
            }.mapNotNull {
            toFirebaseToken(it)
        }
    }

    private fun toFirebaseToken(row: ResultRow): FirebaseToken {
        return FirebaseToken(
            id = row[FirebaseTokens.id],
            token = row[FirebaseTokens.token],
            userId = row[FirebaseTokens.userId]
        )
    }
}