package com.finledger.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "categories",
    indices = [Index("parentId"), Index("isDeleted")],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val type: CategoryType,
    val parentId: Long? = null,
    val isDefault: Boolean = false,
    val isDeleted: Boolean = false,
    val sortOrder: Int = 0,
)

