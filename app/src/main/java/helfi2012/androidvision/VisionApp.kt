package helfi2012.androidvision

import android.app.Application
import android.preference.PreferenceManager
import android.util.Log
import helfi2012.androidvision.ui.SettingsFragment

import helfi2012.androidvision.websocket.BackgroundManager
import helfi2012.androidvision.websocket.SocketConnection

class VisionApp : Application() {
    var socketConnection: SocketConnection? = null
        private set

    val isSocketConnected: Boolean
        get() = socketConnection!!.isConnected

    private var useInternet = true

    private val appActivityListener = object : BackgroundManager.Listener {
        override fun onBecameForeground() {
            openSocketConnection()
            Log.i(TAG, "Became Foreground")
        }

        override fun onBecameBackground() {
            closeSocketConnection()
            Log.i(TAG, "Became Background")
        }
    }

    override fun onCreate() {
        super.onCreate()
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        useInternet = preferences.getBoolean(SettingsFragment.KEY_PREF_USE_INTERNET, true)
        socketConnection = SocketConnection(this)
        BackgroundManager.get(this).registerListener(appActivityListener)
    }

    fun onPreferencesChange() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        useInternet = preferences.getBoolean(SettingsFragment.KEY_PREF_USE_INTERNET, true)
        if (useInternet) {
            socketConnection!!.openConnection()
        } else {
            socketConnection!!.closeConnection()
        }
    }

    fun closeSocketConnection() {
        socketConnection!!.closeConnection()
    }

    fun openSocketConnection() {
        if (useInternet)
            socketConnection!!.openConnection()
    }

    fun reconnect() {
        if (useInternet)
            socketConnection!!.openConnection()
    }

    companion object {
        private val TAG = "VisionApp"
    }
}