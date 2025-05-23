package com.example.tabletopcompanion.network.model

import com.example.tabletopcompanion.data.model.Room

data class StartGameMsg(val initialRoomState: Room) : P2PMessage() {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
