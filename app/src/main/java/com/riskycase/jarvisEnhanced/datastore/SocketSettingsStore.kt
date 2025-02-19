package com.riskycase.jarvisEnhanced.datastore

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import com.riskycase.jarvisEnhanced.SocketSettings
import java.io.InputStream
import java.io.OutputStream

object SocketSettingsStore : Serializer<SocketSettings> {
    override val defaultValue: SocketSettings = SocketSettings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SocketSettings {
        try {
            return SocketSettings.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: SocketSettings, output: OutputStream
    ) = t.writeTo(output)
}

val Context.socketSettings: DataStore<SocketSettings> by dataStore(
    fileName = "socketSettings.pb", serializer = SocketSettingsStore
)
