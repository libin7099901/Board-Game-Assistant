package com.example.tabletopcompanion.network.model

import java.io.Serializable

sealed class P2PMessage : Serializable {
    companion object {
        const val serialVersionUID: Long = 1L
    }
}
