package helfi2012.androidvision.websocket

import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class ClientWebSocket(private val listener: MessageListener, private val host: String) {
    var client: Client? = null
        private set

    var startTime = 0.toLong()

    fun connect() {
        Thread({
            if (client != null) {
                reconnect()
            } else {
                client = Client(URI.create(host))
                client!!.connect()
            }
        }).start()
    }

    private fun reconnect() {
        try {
            client = Client(URI.create(host))
            client!!.connect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun close() {
        client?:return
        client!!.close()
    }

    fun sendMessage(message: String) {
        client?:return
        client!!.send(message)
    }

    fun sendBinary(data: ByteArray) {
        client?:return
        startTime = System.nanoTime()
        client!!.send(data)
    }

    inner class Client(serverUri: URI) : WebSocketClient(serverUri) {
        override fun onOpen(handshakedata: ServerHandshake) {
            Log.d(TAG, "onOpen")
        }

        override fun onMessage(message: String) {
            Log.d(TAG, "Server message: $message; Time: ${((System.nanoTime() - startTime)/Math.pow(10.0,9.0))}")
            listener.onSocketMessage(message)
        }

        override fun onClose(code: Int, reason: String, remote: Boolean) {
            Log.d(TAG, "On close. Reason: " + reason)
        }

        override fun onError(e: Exception) {
            listener.onError()
            e.printStackTrace()
        }
    }

    interface MessageListener {
        fun onSocketMessage(message: String?)
        fun onError()
    }

    companion object {
        private val TAG = "WebSocket"
    }
}