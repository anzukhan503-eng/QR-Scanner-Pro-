package com.example.data.repository

import com.example.data.db.ScanDao
import com.example.data.model.ScanItem
import kotlinx.coroutines.flow.Flow

class ScanRepository(private val scanDao: ScanDao) {
    val allScans: Flow<List<ScanItem>> = scanDao.getAllScans()

    suspend fun insertScan(content: String, qrType: String, displayTitle: String = ""): Long {
        val item = ScanItem(
            content = content,
            qrType = qrType,
            timestamp = System.currentTimeMillis(),
            displayTitle = displayTitle.ifBlank { content.take(40) }
        )
        return scanDao.insertScan(item)
    }

    suspend fun deleteScan(id: Long) {
        scanDao.deleteScanById(id)
    }

    suspend fun clearAll() {
        scanDao.clearAllScans()
    }
}
