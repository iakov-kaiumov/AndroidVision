@file:Suppress("DEPRECATION")

package helfi2012.androidvision.recognision

import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.Camera
import android.os.AsyncTask
import android.preference.PreferenceManager
import android.util.Log
import helfi2012.androidvision.R
import helfi2012.androidvision.utils.ImageUtils
import helfi2012.androidvision.websocket.SocketConnection
import java.io.ByteArrayOutputStream

internal class CameraPreviewCallback(private val assetManager: AssetManager,
                                     private val resources: Resources,
                                     private val socketConnection: SocketConnection,
                                     private val networkListener: NetworkListener) :
        Camera.PreviewCallback, Camera.AutoFocusMoveCallback, SocketConnection.ServerListener {

    companion object {
        private val TAG = "CameraPreviewCallback"

        val BINARY_CONFIG = TensorFlowClassifier.NetworkConfig("Binary Classifier", "opt_food_convnet.pb", R.array.labels1,
                "conv2d_1_input", "dense_2/Softmax", 100, 2)

        val MULTI_CLASS_CONFIG = TensorFlowClassifier.NetworkConfig("Multi-classes Classifier", "graph.pb", R.array.labels2,
                "input_1", "Softmax", 299, 101)
    }

    private var recognizingEnabled = true
    private var binaryTaskIsReadyForNextImage = true
    private var foodTaskIsReadyForNextImage = true
    private var startTime = System.nanoTime()
    private var framesCount = 0
    private var foodCount = 0
    private var bestBitmap: Bitmap? = null
    private var bestScore = 0.toFloat()
    private val modelsMap: HashMap<String, TensorFlowClassifier?> =
            hashMapOf(Pair(BINARY_CONFIG.name, null), Pair(MULTI_CLASS_CONFIG.name, null))

    init {
        socketConnection.registerMessageListener(this)
        LoadModelTask(BINARY_CONFIG).execute()
        LoadModelTask(MULTI_CLASS_CONFIG).execute()
    }

    fun disableNetworks() {
        recognizingEnabled = false
    }

    fun enableNetworks() {
        recognizingEnabled = true
    }

    override fun onAutoFocusMoving(start: Boolean, camera: Camera?) {}

    override fun onPreviewFrame(data: ByteArray, camera: Camera) {
        //Calculate and display FPS in activity
        if (System.nanoTime() - startTime >= Math.pow(10.0, 9.0)) {
            val framesPerSecond = (framesCount / ((System.nanoTime() - startTime) / Math.pow(10.0, 9.0))).toInt()
            startTime = System.nanoTime()
            framesCount = 0
            networkListener.onFrame(framesPerSecond)
        }
        //return if we're are not ready for next image
        if (!binaryTaskIsReadyForNextImage) {
            return
        }
        if (!recognizingEnabled) {
            return
        }
        binaryTaskIsReadyForNextImage = false
        //start AsyncTask
        val task = BinaryClassifierTask(camera, data)
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private inner class LoadModelTask(private val config: TensorFlowClassifier.NetworkConfig)
        : AsyncTask<Void, Void, TensorFlowClassifier?>() {
        override fun doInBackground(vararg params: Void): TensorFlowClassifier? {
            try {
                return TensorFlowClassifier(assetManager, resources, config)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: TensorFlowClassifier?) {
            super.onPostExecute(result)
            networkListener.onNetworkInitialized(config.name, result != null)
            modelsMap.put(config.name, result)
        }
    }

    /**
     * Task that runs TensorFlowClassifier, which recognise whether it is food or not
     * And then displays it in activity
     */
    private inner class BinaryClassifierTask(private val camera: Camera, private val data: ByteArray) :
            AsyncTask<Void, Void, TensorFlowClassifier.Classification?>() {

        private var bitmap: Bitmap? = null
        private val binaryClassifier = modelsMap[BINARY_CONFIG.name]

        override fun doInBackground(vararg params: Void): TensorFlowClassifier.Classification? {
            //return null if classifier is not initialized
            if (binaryClassifier == null) {
                return null
            }
            // Create YuvImage
            val previewSize = camera.parameters.previewSize
            val image = YuvImage(
                    data, camera.parameters.previewFormat, previewSize.width, previewSize.height, null)
            //Convert YuvImage to bitmap
            val out = ByteArrayOutputStream()
            image.compressToJpeg(Rect(0, 0, previewSize.width, previewSize.height), 100, out)
            val bytes = out.toByteArray()
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            return binaryClassifier.recognize(bitmap!!)
        }

        override fun onPostExecute(result: TensorFlowClassifier.Classification?) {
            super.onPostExecute(result)
            if (result == null) {
                binaryTaskIsReadyForNextImage = true
                return
            }
            val score = result.output[1]
            val label = result.labels[1]
            networkListener.onNetworkRecognitionStop(BINARY_CONFIG.name, label, score)
            Thread({
                if (score > 0.5) {
                    if (bitmap == null || score > bestScore) {
                        bestBitmap = bitmap
                        bestScore = score
                    }
                    foodCount++
                    if (foodCount >= 5 && foodTaskIsReadyForNextImage) {
                        foodTaskIsReadyForNextImage = false
                        onFoodDetected(bestBitmap!!)
                        foodCount = 0
                        bestBitmap = null
                        bestScore = 0.toFloat()
                    }
                } else {
                    bitmap = null
                    bestScore = 0.toFloat()
                    foodCount = 0
                }
            }).start()
            //Now we're ready for next image
            binaryTaskIsReadyForNextImage = true
            //Calculate frames per second
            framesCount++
        }
    }

    /**
     * Task that runs TensorFlowClassifier, which recognise food type
     * And then displays it in activity
     */
    private inner class FoodClassifierTask(private val bitmap: Bitmap) : AsyncTask<Void, Void, TensorFlowClassifier.Classification?>() {

        private val foodClassifier = modelsMap[MULTI_CLASS_CONFIG.name]
        private val startTime = System.nanoTime()

        override fun onPreExecute() {
            networkListener.onNetworkRecognitionStart(MULTI_CLASS_CONFIG.name)
            Log.d(TAG, "FoodClassifierTask started")
            super.onPreExecute()
        }

        override fun doInBackground(vararg params: Void): TensorFlowClassifier.Classification? {
            //return null if classifier is not initialized
            if (foodClassifier == null) {
                return null
            }
            return foodClassifier.recognize(bitmap)
        }

        override fun onPostExecute(result: TensorFlowClassifier.Classification?) {
            Log.d(TAG, "FoodClassifierTask done. Time: ${(System.nanoTime() - startTime)/Math.pow(10.0, 9.0)}")
            super.onPostExecute(result)
            if (result == null) {
                foodTaskIsReadyForNextImage = true
                return
            }
            val score = result.output.max()!!
            val index = result.output.indexOf(score)
            val label = result.labels[index]
            networkListener.onNetworkRecognitionStop(MULTI_CLASS_CONFIG.name, label, score)
            //Now we're ready for next image
            foodTaskIsReadyForNextImage = true
        }
    }

    private fun onFoodDetected(bitmap: Bitmap) {
        networkListener.onNetworkRecognitionStart(MULTI_CLASS_CONFIG.name)
        if (socketConnection.isConnected) {
            val cropBitmap = Bitmap.createScaledBitmap(bitmap, MULTI_CLASS_CONFIG.inputSize, MULTI_CLASS_CONFIG.inputSize, false)
            val data = ImageUtils.getBytePixelData(cropBitmap)
            socketConnection.sendBinary(data)
        } else {
            FoodClassifierTask(bitmap).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    override fun onServerResponse(serverMessage: SocketConnection.ServerMessage) {
        networkListener.onNetworkRecognitionStop(MULTI_CLASS_CONFIG.name, serverMessage.label!!, serverMessage.score)
        //Now we're ready for next image
        foodTaskIsReadyForNextImage = true
    }

    interface NetworkListener {
        fun onNetworkInitialized(name: String, successful: Boolean)
        fun onNetworkRecognitionStart(name: String)
        fun onNetworkRecognitionStop(name: String, label: String, score: Float)
        fun onFrame(framesPerSecond: Int)
    }
}