package com.notifytts

import android.app.Application
import com.notifytts.data.PreferencesManager

class NotificationTTSApp : Application() {

    lateinit var preferencesManager: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesManager = PreferencesManager(this)
    }

    companion object {
        lateinit var instance: NotificationTTSApp
            private set
    }
}
