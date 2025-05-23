package com.example.tabletopcompanion.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameTemplateMetadataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: GameTemplateMetadataEntity)

    @Query("SELECT * FROM game_template_metadata ORDER BY importedTimestamp DESC")
    fun getAll(): Flow<List<GameTemplateMetadataEntity>>

    @Query("SELECT * FROM game_template_metadata WHERE templateId = :templateId")
    suspend fun getById(templateId: String): GameTemplateMetadataEntity?

    @Query("DELETE FROM game_template_metadata WHERE templateId = :templateId")
    suspend fun deleteById(templateId: String)

    @Query("DELETE FROM game_template_metadata")
    suspend fun clearAll() // For testing
}
