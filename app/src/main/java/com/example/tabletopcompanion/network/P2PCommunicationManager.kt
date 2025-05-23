package com.example.tabletopcompanion.network

import android.util.Log
import com.example.tabletopcompanion.network.model.P2PMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class P2PCommunicationManager {

    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null
    private val clientOutputStreams = ConcurrentHashMap<String, ObjectOutputStream>()
    private val clientJobs = ConcurrentHashMap<String, Job>()

    private var clientSocket: Socket? = null
    private var clientObjectOutputStream: ObjectOutputStream? = null
    private var clientObjectInputStream: ObjectInputStream? = null
    private var clientListeningJob: Job? = null

    companion object {
        private const val TAG = "P2PManager"
    }

    // --- Host Logic ---

    fun startHostServer(
        port: Int,
        onClientConnected: (outputStream: ObjectOutputStream, clientId: String) -> Unit,
        onClientDisconnected: (clientId: String) -> Unit,
        onMessageReceived: (clientId: String, message: P2PMessage) -> Unit,
        onError: (error: String) -> Unit
    ) {
        if (serverSocket != null && !serverSocket!!.isClosed) {
            Log.w(TAG, "Host server already running.")
            // onError("Host server already running.") // Optionally notify if needed
            return
        }
        managerScope.launch {
            try {
                serverSocket = ServerSocket(port)
                Log.i(TAG, "Host server started on port $port")
                while (isActive && serverSocket?.isClosed == false) {
                    try {
                        val socket = serverSocket!!.accept() // Blocking call
                        val clientId = UUID.randomUUID().toString()
                        Log.i(TAG, "Client connected: $clientId, IP: ${socket.inetAddress}")

                        val objectOutputStream = ObjectOutputStream(socket.getOutputStream())
                        // Flush the stream header to ensure the client can read it immediately.
                        objectOutputStream.flush()
                        clientOutputStreams[clientId] = objectOutputStream
                        onClientConnected(objectOutputStream, clientId)


                        val clientJob = managerScope.launch {
                            try {
                                val objectInputStream = ObjectInputStream(socket.getInputStream())
                                while (isActive) {
                                    try {
                                        val message = objectInputStream.readObject() as P2PMessage
                                        onMessageReceived(clientId, message)
                                    } catch (e: IOException) {
                                        Log.e(TAG, "Client $clientId disconnected: ${e.message}")
                                        break // Exit loop on read error (client disconnected)
                                    } catch (e: ClassNotFoundException) {
                                        Log.e(TAG, "Received unknown object from $clientId: ${e.message}")
                                        // Potentially send an error message back or just log
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in client handler $clientId: ${e.message}", e)
                                // onError("Error handling client $clientId: ${e.message}")
                            } finally {
                                clientOutputStreams.remove(clientId)
                                clientJobs.remove(clientId)
                                onClientDisconnected(clientId)
                                try {
                                    socket.close()
                                } catch (e: IOException) {
                                    Log.e(TAG, "Error closing client socket $clientId: ${e.message}")
                                }
                                Log.i(TAG, "Client $clientId resources cleaned up.")
                            }
                        }
                        clientJobs[clientId] = clientJob

                    } catch (e: IOException) {
                        if (serverSocket?.isClosed == true) {
                            Log.i(TAG, "Server socket closed, exiting accept loop.")
                            break
                        }
                        Log.e(TAG, "Error accepting client connection: ${e.message}", e)
                        onError("Error accepting client: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start host server: ${e.message}", e)
                onError("Failed to start host server: ${e.message}")
            } finally {
                Log.i(TAG, "Host server accept loop finished.")
                stopHostServerInternals() // Ensure cleanup if loop exits unexpectedly
            }
        }
    }

    fun broadcastMessageToAll(message: P2PMessage) {
        if (clientOutputStreams.isEmpty()) {
            Log.i(TAG, "No clients connected, not broadcasting message: $message")
            return
        }
        clientOutputStreams.forEach { (clientId, stream) ->
            try {
                stream.writeObject(message)
                stream.flush() // Ensure message is sent immediately
                Log.i(TAG, "Broadcasted message to $clientId: $message")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to send message to client $clientId: ${e.message}", e)
                // Consider removing client if send fails repeatedly
            }
        }
    }

    fun sendMessageToClient(clientId: String, message: P2PMessage) {
        clientOutputStreams[clientId]?.let { stream ->
            try {
                stream.writeObject(message)
                stream.flush()
                Log.i(TAG, "Sent message to $clientId: $message")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to send message to client $clientId: ${e.message}", e)
            }
        } ?: Log.w(TAG, "Client $clientId not found for sending message.")
    }

    private fun stopHostServerInternals() {
        clientJobs.forEach { (_, job) -> job.cancel() }
        clientJobs.clear()
        clientOutputStreams.forEach { (_, stream) ->
            try {
                stream.close()
            } catch (e: IOException) { /* Log error */
                Log.e(TAG, "Error closing client stream: ${e.message}")
            }
        }
        clientOutputStreams.clear()
        serverSocket?.let {
            if (!it.isClosed) {
                try {
                    it.close()
                    Log.i(TAG, "Server socket closed.")
                } catch (e: IOException) {
                    Log.e(TAG, "Error closing server socket: ${e.message}", e)
                }
            }
            serverSocket = null
        }
    }

    fun stopHostServer() {
        Log.i(TAG, "Stopping host server...")
        stopHostServerInternals()
        // managerScope.cancel() // This would cancel all coroutines including client connection handlers
        // Instead, rely on individual job cancellation and serverSocket.close() to stop accept loop.
    }


    // --- Client Logic ---

    fun connectToHost(
        hostAddress: String,
        port: Int,
        onConnectionSuccess: (outputStream: ObjectOutputStream, inputStream: ObjectInputStream) -> Unit,
        onMessageReceived: (message: P2PMessage) -> Unit,
        onDisconnected: () -> Unit,
        onError: (error: String) -> Unit
    ) {
        if (clientSocket != null && clientSocket!!.isConnected) {
            Log.w(TAG, "Client already connected.")
            // onError("Client already connected.")
            return
        }
        managerScope.launch {
            try {
                val address = InetAddress.getByName(hostAddress)
                clientSocket = Socket(address, port)
                Log.i(TAG, "Connected to host: $hostAddress:$port")

                clientObjectOutputStream = ObjectOutputStream(clientSocket!!.getOutputStream())
                clientObjectOutputStream!!.flush() // Send header

                clientObjectInputStream = ObjectInputStream(clientSocket!!.getInputStream())

                onConnectionSuccess(clientObjectOutputStream!!, clientObjectInputStream!!)

                clientListeningJob = managerScope.launch {
                    try {
                        while (isActive) {
                            val message = clientObjectInputStream!!.readObject() as P2PMessage
                            onMessageReceived(message)
                        }
                    } catch (e: IOException) {
                        Log.e(TAG, "Disconnected from host: ${e.message}")
                        onDisconnected()
                    } catch (e: ClassNotFoundException) {
                        Log.e(TAG, "Received unknown object from host: ${e.message}")
                        onError("Received unknown object from host: ${e.message}")
                    } finally {
                        disconnectFromHostInternals() // Clean up if listening loop ends
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to host: ${e.message}", e)
                onError("Connection failed: ${e.message}")
                disconnectFromHostInternals()
            }
        }
    }

    fun sendMessageToServer(message: P2PMessage) {
        clientObjectOutputStream?.let { stream ->
            managerScope.launch { // Send in a coroutine to avoid blocking UI thread
                try {
                    stream.writeObject(message)
                    stream.flush()
                    Log.i(TAG, "Sent message to server: $message")
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to send message to server: ${e.message}", e)
                    // Potentially trigger disconnect logic
                }
            }
        } ?: Log.w(TAG, "Not connected to server, cannot send message.")
    }

    private fun disconnectFromHostInternals() {
        clientListeningJob?.cancel()
        clientListeningJob = null
        try {
            clientObjectInputStream?.close()
        } catch (e: IOException) { Log.e(TAG, "Error closing client input stream: ${e.message}") }
        clientObjectInputStream = null
        try {
            clientObjectOutputStream?.close()
        } catch (e: IOException) { Log.e(TAG, "Error closing client output stream: ${e.message}") }
        clientObjectOutputStream = null
        try {
            clientSocket?.close()
        } catch (e: IOException) { Log.e(TAG, "Error closing client socket: ${e.message}") }
        clientSocket = null
        Log.i(TAG, "Client disconnected and resources cleaned up.")
    }

    fun disconnectFromHost() {
        Log.i(TAG, "Disconnecting from host...")
        disconnectFromHostInternals()
    }

    // Call this when the P2PCommunicationManager is no longer needed (e.g., ViewModel onCleared)
    fun shutdown() {
        Log.i(TAG, "Shutting down P2PCommunicationManager.")
        stopHostServer()
        disconnectFromHost()
        managerScope.cancel() // Cancel all coroutines in the scope
    }
}
