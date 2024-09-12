package com.riskycase.jarvisEnhanced.viewModel

import android.app.AppOpsManager
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import com.riskycase.jarvisEnhanced.service.NotificationListener

class SettingsViewModel(private val application: Application) : AndroidViewModel(application) {

    fun getNotificationListenerServiceEnabled(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(application.applicationContext)
            .contains(application.packageName)
    }

    fun openNotificationListenerSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
        intent.putExtra(
            Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, ComponentName(
                application.packageName, NotificationListener::class.java.name
            ).flattenToString()
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(
            intent
        )
    }

    fun getUsageAccessEnabled(): Boolean {
        return application.applicationContext.getSystemService(AppOpsManager::class.java)
            .unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), application.packageName
            ) == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(
            intent
        )
    }

    fun refreshSnaps() {
        NotificationListener().readPendingSnaps(application.applicationContext, application)
    }

    fun restartService() {
        // FIXME: This crashes the app with java.lang.NullPointerException: class name is null
//        NotificationListener().goForeground(application.applicationContext)
    }

}