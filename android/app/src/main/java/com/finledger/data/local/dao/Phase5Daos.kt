package com.finledger.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.finledger.data.local.entity.BackupHistoryEntity
import com.finledger.data.local.entity.DocumentEntity
import com.finledger.data.local.entity.ImportHistoryEntity
import com.finledger.data.local.entity.ImportStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id AND isDeleted = 0")
    fun observeById(id: Long): Flow<DocumentEntity?>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE linkedEntityType = :entityType AND linkedEntityId = :entityId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun observeByLinkedEntity(entityType: String, entityId: Long): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents")
    suspend fun getAllRawOnce(): List<DocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(doc: DocumentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(docs: List<DocumentEntity>)

    @Query("UPDATE documents SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("UPDATE documents SET isDeleted = 0 WHERE id = :id")
    suspend fun restore(id: Long)
}

@Dao
interface ImportHistoryDao {
    @Query("SELECT * FROM import_history ORDER BY importedAt DESC")
    fun observeAll(): Flow<List<ImportHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: ImportHistoryEntity): Long

    @Query("UPDATE import_history SET status = :status, canRollback = 0, rolledBackAt = :rolledBackAt WHERE id = :id")
    suspend fun markRolledBack(id: Long, status: ImportStatus = ImportStatus.ROLLED_BACK, rolledBackAt: Long = System.currentTimeMillis())
}

@Dao
interface BackupHistoryDao {
    @Query("SELECT * FROM backup_history ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BackupHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: BackupHistoryEntity): Long
}

