package me.hdraganovski.shout.liveservice.service

import me.hdraganovski.shout.liveservice.model.User
import me.hdraganovski.shout.liveservice.model.Users
import me.hdraganovski.shout.liveservice.service.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.*

object UserService {

    suspend fun getUser(key: Long): User? = dbQuery {
        Users.select {
            Users.id eq key
        }.mapNotNull {
            toUser(it)
        }.singleOrNull()
    }

    suspend fun getKeyForServerID(id: Long): Long? = dbQuery {
        Users.select {
            Users.id eq id
        }.mapNotNull {
            it[Users.id]
        }.singleOrNull()
    }

    suspend fun getUserForServerID(id: Long): User? = dbQuery {
        Users.select {
            Users.id eq id
        }.mapNotNull {
            toUser(it)
        }.singleOrNull()
    }

    suspend fun addUser(_id: Long): User {
        var key: Long? = null
        dbQuery {
            key = (Users.insert {
                it[id] = _id
            } get Users.id)
        }
        return getUser(key!!)!!
    }

    suspend fun getAll(): List<User> = dbQuery {
        Users.selectAll().mapNotNull {
            toUser(it)
        }
    }

    suspend fun addUser(user: User): User {
        return addUser(user.id)
    }

    suspend fun removeUser(serverId: Long): Boolean = dbQuery {
           Users.deleteWhere {
               Users.id eq serverId
           } > 0
    }

    fun toUser(row: ResultRow): User {
        return User(
            id = row[Users.id]
        )
    }
}