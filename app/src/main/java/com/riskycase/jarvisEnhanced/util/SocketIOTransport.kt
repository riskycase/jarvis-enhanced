package com.riskycase.jarvisEnhanced.util

import android.content.Context
import android.os.BatteryManager
import com.google.gson.Gson
import com.riskycase.jarvisEnhanced.datastore.socketSettings
import com.riskycase.jarvisEnhanced.util.SocketIOConstants.AUTH_TOKEN
import com.riskycase.jarvisEnhanced.util.SocketIOConstants.DEVICE_ID
import com.riskycase.jarvisEnhanced.util.SocketIOConstants.DEVICE_SECRET
import dagger.hilt.android.qualifiers.ApplicationContext
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.net.URI
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject

class SocketIOTransport @Inject constructor(@ApplicationContext val applicationContext: Context) {

    private var socket: Socket? = null
    private var authToken: String? = null
    private var socketAuthenticated = false

    private val messageList = HashMap<String, Map<String, String>>()
    private var messageLock = ReentrantLock(true)

    private val gson: Gson = Gson()

    init {
        resetSocket()
    }

    fun resetSocket() {
        socket?.disconnect()
        socketAuthenticated = false
        runBlocking {
            val settings = applicationContext.socketSettings.data.first()
            val uri = URI.create(settings.serverUrl)
            val options = IO.Options.builder().setAuth(
                mapOf(
                    Pair(DEVICE_ID, settings.deviceId), Pair(DEVICE_SECRET, settings.deviceSecret)
                )
            ).setReconnection(true).build()
            socket = IO.socket(uri, options)
            socket?.onAnyIncoming { messages ->
                if (AUTH_TOKEN == messages[0]) {
                    authToken = gson.fromJson(
                        messages[1].toString(), SocketIOConstants.AuthTokenBody::class.java
                    ).authToken
                    socketAuthenticated = true

                    synchronized(messageLock, {
                        messageList.entries.forEach { entry ->
                            publishMessageOnChannel(
                                entry.key, entry.value
                            )
                        }
                    })
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
        synchronized(messageLock, {
            messageList[channel] = message
            publishMessageOnChannel(channel, message)
        })
    }
}