package com.example.data.repository

import com.example.data.db.GeneratedDao
import com.example.data.model.GeneratedItem
import kotlinx.coroutines.flow.Flow

class GeneratedRepository(private val generatedDao: GeneratedDao) {
    val allGenerated: Flow<List<GeneratedItem>> = generatedDao.getAllGenerated()

    suspend fun insertGenerated(qrType: String, content: String, title: String = ""): Long {
        val item = GeneratedItem(
            qrType = qrType,
            content = content,
            timestamp = System.currentTimeMillis(),
            title = title.ifBlank { content.take(40) }
        )
        return generatedDao.insertGenerated(item)
    }

    suspend fun deleteGenerated(id: Long) {
        generatedDao.deleteGeneratedById(id)
    }

    suspend fun clearAll() {
        generatedDao.clearAllGenerated()
    }
}
