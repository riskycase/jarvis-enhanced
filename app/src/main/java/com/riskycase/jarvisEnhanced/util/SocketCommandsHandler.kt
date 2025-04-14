package com.riskycase.jarvisEnhanced.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.riskycase.jarvisEnhanced.util.SocketIOConstants.CommandCategories.MUSIC
import com.riskycase.jarvisEnhanced.util.wallpaper.MediaUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class SocketCommandsHandler : BroadcastReceiver() {

    @Inject
    lateinit var mediaUtils: MediaUtils

    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent?.action == "com.riskycase.jarvisEnhanced.command") {
            intent.extras?.getString("command", "")?.let { handleCommand(it) }
        }
    }

    private fun handleCommand(command: String) {
        if ("" == command) return
        val parts = command.split("=:=")
        if (MUSIC == parts[1]) {
            mediaUtils.handleSocketCommand(parts)
        }
    }

}