package com.example.tabletopcompanion.network.model

data class ErrorMessageMsg(val error: String) : P2PMessage() {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
