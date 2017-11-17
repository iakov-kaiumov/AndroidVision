package helfi2012.androidvision.recognision

import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.Bitmap
import helfi2012.androidvision.utils.ImageUtils
import org.tensorflow.contrib.android.TensorFlowInferenceInterface

internal class TensorFlowClassifier(assetManager: AssetManager, resources: Resources, private val config: NetworkConfig) {
    companion object {
        private val TAG = "TensorFlowClassifier"
    }

    private var tfHelper: TensorFlowInferenceInterface? = null
    private var outputNames: Array<String>? = null
    private var output: FloatArray? = null
    private var labels: Array<String>? = null

    init {
        //set its model path and where the raw asset files are
        tfHelper = TensorFlowInferenceInterface(assetManager, config.modelPath)
        labels = resources.getStringArray(config.labelId)
        outputNames = arrayOf(config.outputName)
        output = FloatArray(config.numClasses)
    }

    fun recognize(bitmap: Bitmap): Classification {
        val cropBitmap = Bitmap.createScaledBitmap(bitmap, config.inputSize, config.inputSize, false)
        val data = ImageUtils.getFloatPixelData(cropBitmap)
        tfHelper!!.feed(
                config.inputName,
                data,
                1.toLong(),
                config.inputSize.toLong(),
                config.inputSize.toLong(),
                ImageUtils.RGB_CHANNELS.toLong()
        )

        //get the possible outputs
        tfHelper!!.run(outputNames)

        //get the output
        tfHelper!!.fetch(config.outputName, output)

        //Log.d(TAG, config.name + Arrays.toString(output))
        return Classification(output!!, labels!!)
    }

    open class Classification(val output: FloatArray, val labels: Array<String>)
    open class NetworkConfig(val name: String, val modelPath: String, val labelId: Int, val inputName: String,
                             val outputName: String, val inputSize: Int, val numClasses: Int)
}