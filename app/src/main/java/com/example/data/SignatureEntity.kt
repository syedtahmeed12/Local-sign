package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "signatures")
data class SignatureEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "DRAW" or "TEXT"
    val content: String, // points serialized path for DRAW, plain text string for TEXT
    val styleIndex: Int = 0, // Font Style Selection index for typed text signatures
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface SignatureDao {
    @Query("SELECT * FROM signatures ORDER BY timestamp DESC")
    fun getAllSignatures(): Flow<List<SignatureEntity>>

    @Query("SELECT * FROM signatures WHERE id = :id LIMIT 1")
    suspend fun getSignatureById(id: Int): SignatureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignature(signature: SignatureEntity): Long

    @Delete
    suspend fun deleteSignature(signature: SignatureEntity)
}
