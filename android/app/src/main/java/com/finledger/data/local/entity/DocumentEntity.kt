package com.finledger.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "documents",
    indices = [Index("type"), Index("linkedEntityType", "linkedEntityId"), Index("isDeleted")],
)
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: DocumentType,
    val fileUri: String,
    val mimeType: String,
    val fileSizeBytes: Long = 0L,
    val linkedEntityType: String = "",
    val linkedEntityId: Long? = null,
    val tags: String = "",
    val notes: String = "",
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "import_history")
data class ImportHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val importedAt: Long = System.currentTimeMillis(),
    val totalRows: Int,
    val successRows: Int,
    val failedRows: Int,
    val duplicateRows: Int,
    val status: ImportStatus,
    val errorFileUri: String = "",
    val canRollback: Boolean = false,
    val rolledBackAt: Long? = null,
)

@Serializable
@Entity(tableName = "backup_history")
data class BackupHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val fileUri: String,
    val fileSizeBytes: Long = 0L,
    val isScheduled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

