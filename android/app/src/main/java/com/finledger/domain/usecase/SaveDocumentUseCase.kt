package com.finledger.domain.usecase

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.finledger.data.local.dao.DocumentDao
import com.finledger.data.local.entity.DocumentEntity
import com.finledger.data.local.entity.DocumentType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject

class SaveDocumentUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val documentDao: DocumentDao,
) {
    fun observeAll(): Flow<List<DocumentEntity>> = documentDao.observeAll()
    fun observeById(id: Long): Flow<DocumentEntity?> = documentDao.observeById(id)

    suspend fun importFromUri(
        uri: Uri,
        name: String,
        type: DocumentType,
        linkedEntityType: String = "",
        linkedEntityId: Long? = null,
        notes: String = "",
    ): Result<Long> = runCatching {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        val origName = queryDisplayName(uri) ?: "doc_${System.currentTimeMillis()}"
        val vaultDir = File(context.filesDir, "vault").apply { mkdirs() }
        val destFile = File(vaultDir, "${System.currentTimeMillis()}_$origName")

        resolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Unable to open selected file")

        documentDao.insert(
            DocumentEntity(
                name = name.ifBlank { origName },
                type = type,
                fileUri = destFile.absolutePath,
                mimeType = mimeType,
                fileSizeBytes = destFile.length(),
                linkedEntityType = linkedEntityType,
                linkedEntityId = linkedEntityId,
                notes = notes.trim(),
            )
        )
    }

    suspend fun softDelete(id: Long) = documentDao.softDelete(id)
    suspend fun restore(id: Long) = documentDao.restore(id)

    private fun queryDisplayName(uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) return cursor.getString(idx)
        }
        return null
    }
}

