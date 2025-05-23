package com.example.tabletopcompanion.network.model

import com.example.tabletopcompanion.data.model.Room

data class RoomStateUpdate(val room: Room) : P2PMessage() {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
