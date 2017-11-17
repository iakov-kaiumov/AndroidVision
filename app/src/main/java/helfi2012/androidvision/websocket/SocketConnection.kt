package helfi2012.androidvision.websocket

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.util.Log
import com.google.gson.GsonBuilder

class SocketConnection(private val context: Context) : ClientWebSocket.MessageListener {
    companion object {
        private val HOST = "ws://192.168.43.114:8000/websocket"
    }
    private var clientWebSocket: ClientWebSocket? = null
    private val gson = GsonBuilder().create()
    private val socketConnectionHandler: Handler = Handler()
    private var serverListener: ServerListener? = null

    private val checkConnectionRunnable = Runnable {
        if (clientWebSocket!!.client == null || !clientWebSocket!!.client!!.isOpen) {
            openConnection()
        }
        startCheckConnection()
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_ON) {
                openConnection()
            } else
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                closeConnection()
            }
        }
    }

    val isConnected: Boolean
        get() = clientWebSocket != null &&
                clientWebSocket!!.client != null &&
                clientWebSocket!!.client!!.isOpen

    private fun startCheckConnection() {
        socketConnectionHandler.postDelayed(checkConnectionRunnable, 5000)
    }

    private fun stopCheckConnection() {
        socketConnectionHandler.removeCallbacks(checkConnectionRunnable)
    }

    fun openConnection() {
        if (clientWebSocket != null) clientWebSocket!!.close()
        try {
            clientWebSocket = ClientWebSocket(this, HOST)
            clientWebSocket!!.connect()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        initScreenStateListener()
        startCheckConnection()
    }

    fun closeConnection() {
        if (clientWebSocket != null) {
            clientWebSocket!!.close()
            clientWebSocket = null
        }
        releaseScreenStateListener()
        stopCheckConnection()
    }

    fun registerMessageListener(serverListener: ServerListener) {
        this.serverListener = serverListener
    }

    fun sendMessage(message: String) {
        if (isConnected) {
            clientWebSocket!!.sendMessage(message)
        }
    }

    fun sendBinary(data: ByteArray) {
        if (isConnected) {
            Log.d("TAG", "SEND BINARY")
            clientWebSocket!!.sendBinary(data)
        }
    }

    override fun onError() {
        stopCheckConnection()
        clientWebSocket = null
        openConnection()
    }

    override fun onSocketMessage(message: String?) {
        if (serverListener != null) {
            val serverMessage = gson.fromJson(message, ServerMessage::class.java)
            serverListener!!.onServerResponse(serverMessage)
        }
    }

    /**
     * Screen state listener for socket lifecycle
     */
    private fun initScreenStateListener() {
        context.registerReceiver(screenStateReceiver, IntentFilter(Intent.ACTION_SCREEN_ON))
        context.registerReceiver(screenStateReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    private fun releaseScreenStateListener() {
        try {
            context.unregisterReceiver(screenStateReceiver)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }

    }

    class ServerMessage {
        var label: String? = null
        var score: Float = 0.toFloat()
    }

    interface ServerListener {
        fun onServerResponse(serverMessage: ServerMessage)
    }
}