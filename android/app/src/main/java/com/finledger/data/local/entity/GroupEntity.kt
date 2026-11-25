package com.finledger.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val imageUri: String = "",
    val currency: String = "INR",
    val spendingLimit: Long? = null,
    val status: GroupStatus = GroupStatus.ACTIVE,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "group_members",
    primaryKeys = ["groupId", "personId"],
    indices = [Index("personId")],
)
data class GroupMemberEntity(
    val groupId: Long,
    val personId: Long,
    val role: String = "MEMBER",
    val joinedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
)

