package com.example.tabletopcompanion.network.model

data class JoinRequest(val username: String, val userId: String) : P2PMessage() {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
