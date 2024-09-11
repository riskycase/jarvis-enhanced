package com.riskycase.jarvisEnhanced.datastore

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import com.riskycase.jarvisEnhanced.SettingsDataStore
import java.io.InputStream
import java.io.OutputStream

object SettingsSerializer : Serializer<SettingsDataStore> {
    override val defaultValue: SettingsDataStore = SettingsDataStore.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SettingsDataStore {
        try {
            return SettingsDataStore.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: SettingsDataStore, output: OutputStream
    ) = t.writeTo(output)
}

val Context.settingsDataStore: DataStore<SettingsDataStore> by dataStore(
    fileName = "settings.pb", serializer = SettingsSerializer
)
