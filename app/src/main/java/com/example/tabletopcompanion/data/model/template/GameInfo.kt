package com.example.tabletopcompanion.data.model.template

import com.google.gson.annotations.SerializedName

data class GameInfo(
    val name: String,
    val author: String?,
    val description: String?,
    @SerializedName("player_count") val playerCount: List<Int> // [min, max] or specific counts like [2,4]
)
