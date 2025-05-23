package com.example.tabletopcompanion.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.tabletopcompanion.data.model.GameTemplateMetadata // Domain model

@Entity(tableName = "game_template_metadata")
data class GameTemplateMetadataEntity(
    @PrimaryKey val templateId: String,
    val templateVersion: String,
    val name: String,
    val author: String?,
    val description: String?,
    val playerCountDescription: String,
    val filePath: String, // Relative path within unzipped dir
    val unzippedDirectoryPath: String, // Absolute path to unzipped dir
    val importedTimestamp: Long = System.currentTimeMillis()
)

// Conversion functions (can be extension functions or in a mapper class)
fun GameTemplateMetadataEntity.toDomainModel(): GameTemplateMetadata {
    return GameTemplateMetadata(
        templateId = templateId,
        templateVersion = templateVersion,
        name = name,
        author = author,
        description = description,
        playerCountDescription = playerCountDescription,
        filePath = filePath,
        unzippedDirectoryPath = unzippedDirectoryPath,
        // importedTimestamp is not in domain model currently, can be added if needed
    )
}

fun GameTemplateMetadata.toEntityModel(): GameTemplateMetadataEntity {
    return GameTemplateMetadataEntity(
        templateId = templateId,
        templateVersion = templateVersion,
        name = name,
        author = author,
        description = description,
        playerCountDescription = playerCountDescription,
        filePath = filePath,
        unzippedDirectoryPath = unzippedDirectoryPath
        // importedTimestamp will be set by Entity's default
    )
}
