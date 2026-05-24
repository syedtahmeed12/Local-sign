package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val signatureDao: SignatureDao,
    private val documentDao: DocumentDao
) {
    // Flows for reactive UI updates
    val allSignatures: Flow<List<SignatureEntity>> = signatureDao.getAllSignatures()
    val allDocuments: Flow<List<DocumentEntity>> = documentDao.getAllDocuments()

    // Signatures Operations
    suspend fun getSignatureById(id: Int): SignatureEntity? {
        return signatureDao.getSignatureById(id)
    }

    suspend fun insertSignature(signature: SignatureEntity): Long {
        return signatureDao.insertSignature(signature)
    }

    suspend fun deleteSignature(signature: SignatureEntity) {
        signatureDao.deleteSignature(signature)
    }

    // Documents Operations
    suspend fun getDocumentById(id: Int): DocumentEntity? {
        return documentDao.getDocumentById(id)
    }

    suspend fun insertDocument(document: DocumentEntity): Long {
        return documentDao.insertDocument(document)
    }

    suspend fun deleteDocumentById(id: Int) {
        documentDao.deleteDocumentById(id)
    }

    suspend fun clearAllDocuments() {
        documentDao.deleteAll()
    }
}
