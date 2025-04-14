package com.riskycase.jarvisEnhanced.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import com.riskycase.jarvisEnhanced.util.SocketChannels.BATTERY_DETAILS
import com.riskycase.jarvisEnhanced.util.SocketChannels.BatteryDetails
import com.riskycase.jarvisEnhanced.util.SocketChannels.DELTA_BATTERY_DETAILS
import com.riskycase.jarvisEnhanced.util.SocketIOTransport
import com.riskycase.jarvisEnhanced.util.SystemServicesContainer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@AndroidEntryPoint
class BatteryEventsBroadcastReceiver @Inject constructor() : BroadcastReceiver() {

    @Inject
    lateinit var socketIOTransport: SocketIOTransport

    @Inject
    lateinit var batteryManager: BatteryManager

    @Inject
    lateinit var systemServicesContainer: SystemServicesContainer

    private val batteryDetailsMap = HashMap<String, String>()

    fun sendBatteryInfoUpdate() {
        socketIOTransport.sendMessage(BATTERY_DETAILS, batteryDetailsMap)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        systemServicesContainer.batteryEventsBroadcastReceiver = this
        if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
            batteryDetailsMap[BatteryDetails.BATTERY_LEVEL] =
                intent.extras?.getInt(BatteryManager.EXTRA_LEVEL).toString()
            batteryDetailsMap[BatteryDetails.BATTERY_STATUS] =
                intent.extras?.getInt(BatteryManager.EXTRA_STATUS).toString()
            batteryDetailsMap[BatteryDetails.BATTERY_TEMPERATURE] =
                intent.extras?.getInt(BatteryManager.EXTRA_TEMPERATURE).toString()
            batteryDetailsMap[BatteryDetails.BATTERY_HEALTH] =
                intent.extras?.getInt(BatteryManager.EXTRA_HEALTH).toString()
            batteryDetailsMap[BatteryDetails.BATTERY_POWER_SOURCE] =
                intent.extras?.getInt(BatteryManager.EXTRA_PLUGGED).toString()
            batteryDetailsMap[BatteryDetails.BATTERY_CURRENT] =
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                    .toString()
            batteryDetailsMap[BatteryDetails.BATTERY_VOLTAGE] =
                intent.extras?.getInt(BatteryManager.EXTRA_VOLTAGE).toString()
        } else if (intent?.action == BatteryManager.ACTION_DISCHARGING) {
            socketIOTransport.sendMessage(
                DELTA_BATTERY_DETAILS, mutableMapOf(
                    Pair(BatteryDetails.BATTERY_STATUS, "3"),
                )
            )
        } else if (intent?.action == BatteryManager.ACTION_CHARGING) {
            socketIOTransport.sendMessage(
                DELTA_BATTERY_DETAILS, mutableMapOf(
                    Pair(BatteryDetails.BATTERY_STATUS, "1"),
                )
            )
        }
    }
}