package com.riskycase.jarvisEnhanced.util

import com.riskycase.jarvisEnhanced.service.NotificationListener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationListenerConnector @Inject constructor() {
    var notificationListener: NotificationListener? = null
}