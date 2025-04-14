package com.riskycase.jarvisEnhanced.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.gson.Gson
import com.riskycase.jarvisEnhanced.datastore.socketSettings
import com.riskycase.jarvisEnhanced.util.SocketIOConstants.AUTH_TOKEN
import com.riskycase.jarvisEnhanced.util.SocketIOConstants.COMMAND
import com.riskycase.jarvisEnhanced.util.SocketIOConstants.DEVICE_ID
import com.riskycase.jarvisEnhanced.util.SocketIOConstants.DEVICE_SECRET
import dagger.hilt.android.qualifiers.ApplicationContext
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketIOTransport @Inject constructor(
    @ApplicationContext val applicationContext: Context,
    private val gson: Gson
) {

    private var socket: Socket? = null
    private var authToken: String? = null
    private var socketAuthenticated = false

    private val messageList = ConcurrentHashMap<String, Map<String, String>>()

    init {
        resetSocket()
    }

    fun resetSocket() {
        socket?.disconnect()
        socketAuthenticated = false
        runBlocking {
            val settings = applicationContext.socketSettings.data.first()
            val uri = URI.create(if(settings.hasServerUrl()) settings.serverUrl else "http://localhost")
            val options = IO.Options.builder().setAuth(
                mapOf(
                    Pair(DEVICE_ID, settings.deviceId), Pair(DEVICE_SECRET, settings.deviceSecret)
                )
            ).setReconnection(true).setReconnectionAttempts(Int.MAX_VALUE).build()
            socket = IO.socket(uri, options)
            socket?.onAnyIncoming { messages ->
                if (AUTH_TOKEN == messages[0]) {
                    authToken = gson.fromJson(
                        messages[1].toString(), SocketIOConstants.AuthTokenBody::class.java
                    ).authToken
                    socketAuthenticated = true
                    messageList.entries.forEach { entry ->
                        publishMessageOnChannel(
                            entry.key, entry.value
                        )
                    }
                } else if (COMMAND == messages[0]) {
                    Log.i("SocketTransport", "Received command ${messages[1]}")
                    val startIntent = Intent("com.riskycase.jarvisEnhanced.command")
                    startIntent.putExtra("command", messages[1].toString())
                    startIntent.setPackage(applicationContext.packageName)
                    applicationContext.sendBroadcast(startIntent)
                }
            }
            socket?.connect()
        }
    }

    private fun publishMessageOnChannel(channel: String, message: Map<String, String>) {
        if (socket?.connected() == true && socketAuthenticated) socket?.also {
            it.emit(
                channel, gson.toJson(
                    mapOf(Pair(AUTH_TOKEN, authToken), Pair("body", gson.toJson(message)))
                )
            )
            messageList.remove(channel)
        }
    }

    fun sendMessage(channel: String, message: Map<String, String>) {
        messageList[channel] = message
        publishMessageOnChannel(channel, message)
    }
}