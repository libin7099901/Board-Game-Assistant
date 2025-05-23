package com.example.tabletopcompanion.network.model

import com.example.tabletopcompanion.data.model.Player

data class PlayerJoinedMsg(val player: Player) : P2PMessage() {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
