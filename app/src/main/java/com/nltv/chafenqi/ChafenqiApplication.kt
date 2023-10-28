package com.nltv.chafenqi

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.nltv.chafenqi.storage.room.RoomContainer
import com.nltv.chafenqi.storage.room.RoomDataContainer
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val Context.cacheStore: DataStore<Preferences> by preferencesDataStore(name = "cacheStore")
const val ONE_SIGNAL_APP_ID = "61d8cb1c-6de2-4b50-af87-f419b2d24ece"

class ChafenqiApplication: Application() {
    lateinit var container: RoomContainer

    init {
        instance = this
    }

    companion object {
        private var instance: ChafenqiApplication? = null

        fun applicationContext() : Context {
            return requireNotNull(instance).applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        container = RoomDataContainer(this)
        if (!::container.isInitialized) {
            println("Successfully initialized room data container.")
        }

        // OneSignal setup
        OneSignal.Debug.logLevel = LogLevel.VERBOSE
        OneSignal.initWithContext(this, ONE_SIGNAL_APP_ID)
        CoroutineScope(Dispatchers.IO).launch {
            OneSignal.Notifications.requestPermission(true)
        }
    }
}