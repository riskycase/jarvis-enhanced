package com.riskycase.jarvisEnhanced.viewModel

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import com.riskycase.jarvisEnhanced.service.NotificationListener
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val notificationListener: NotificationListener
) : ViewModel() {

    fun getNotificationListenerServiceEnabled(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(applicationContext)
            .contains(applicationContext.packageName)
    }

    fun openNotificationListenerSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
        intent.putExtra(
            Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, ComponentName(
                applicationContext.packageName, NotificationListener::class.java.name
            ).flattenToString()
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        applicationContext.startActivity(
            intent
        )
    }

    fun getUsageAccessEnabled(): Boolean {
        return applicationContext.getSystemService(AppOpsManager::class.java).unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), applicationContext.packageName
        ) == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        applicationContext.startActivity(
            intent
        )
    }

    fun refreshSnaps() {
        notificationListener.readPendingSnaps()
    }

    fun restartService() {
        // FIXME: This crashes the app with java.lang.NullPointerException: class name is null
//        NotificationListener().goForeground(application.applicationContext)
    }

}