package com.example.tabletopcompanion.model

data class GameTemplateInfo(
    val id: String,
    val name: String,
    val description: String?,
    val version: String?,
    val unzippedPath: String,
    val originalZipName: String?,
    val phases: List<String> = emptyList(),
    val initialIndicators: List<com.example.tabletopcompanion.model.template.ParsedIndicatorInfo> = emptyList()
)
