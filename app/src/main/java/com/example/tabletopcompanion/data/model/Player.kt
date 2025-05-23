package com.example.tabletopcompanion.data.model

import java.io.Serializable

data class Player(
    val id: String,
    val username: String,
    val isHost: Boolean = false,
    val avatarUrl: String? = null
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
