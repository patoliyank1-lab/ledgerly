package com.finledger.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL,
        )
    ],
    indices = [Index("groupId"), Index("isDeleted")],
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val location: String = "",
    val groupId: Long? = null,
    val startDate: Long,
    val endDate: Long? = null,
    val budgetAmount: Long? = null,
    val notes: String = "",
    val status: EventStatus = EventStatus.OPEN,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "event_participants",
    primaryKeys = ["eventId", "personId"],
    indices = [Index("personId")],
)
data class EventParticipantEntity(
    val eventId: Long,
    val personId: Long,
    val isActive: Boolean = true,
)

