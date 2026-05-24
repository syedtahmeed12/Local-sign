package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val docType: String, // "TEXT" or "PDF"
    val originalContent: String, // User text inside the textbox or mock PDF text/metadata
    val fileSize: Long = 0L, // Size of text or file in bytes
    val encryptedContent: String, // AES-encrypted representation of document content
    val encryptionIv: String, // Base64 AES Initialization Vector
    val checksumSha256: String, // SHA-256 fingerprint of the document
    val signatureBlock: String, // Holds point coordinates or visual marker of signature
    val signatureType: String, // "DRAW", "TEXT", "SAVED"
    val signatureName: String, // Name of signer or name of saved signature used
    val auditTrailJson: String, // Serialize chronological event logs
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY timestamp DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Int): DocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Int)

    @Query("DELETE FROM documents")
    suspend fun deleteAll()
}
