package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.GeneratedItem
import kotlinx.coroutines.flow.Flow

@Dao
interface GeneratedDao {
    @Query("SELECT * FROM generated_history ORDER BY timestamp DESC")
    fun getAllGenerated(): Flow<List<GeneratedItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenerated(item: GeneratedItem): Long

    @Query("DELETE FROM generated_history WHERE id = :id")
    suspend fun deleteGeneratedById(id: Long)

    @Query("DELETE FROM generated_history")
    suspend fun clearAllGenerated()
}
