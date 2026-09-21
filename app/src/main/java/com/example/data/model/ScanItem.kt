package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val qrType: String, // URL, PHONE, EMAIL, WIFI, CONTACT, SMS, TEXT
    val timestamp: Long = System.currentTimeMillis(),
    val displayTitle: String = ""
)
