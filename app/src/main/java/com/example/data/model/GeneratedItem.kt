package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "generated_history")
data class GeneratedItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val qrType: String, // TEXT, URL, PHONE, EMAIL, WIFI, CONTACT, SMS
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String = ""
)
