package helfi2012.androidvision.recognition

import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.Bitmap
import org.tensorflow.contrib.android.TensorFlowInferenceInterface

/**
 * This abstract class implements work with tensorFlowInferenceInterface.
 * You have to extend this class to write your own.
 * In imagePreprocessing function implement your logic.
 */

abstract class TensorFlowClassifier(protected val config: NetworkConfig, assetManager: AssetManager, labels: Array<String>) {

    private var tensorFlowInferenceInterface: TensorFlowInferenceInterface? = null
    private var outputNames: Array<String>? = null
    private var output: FloatArray? = null
    private var labels: Array<String>? = null

    constructor (config: NetworkConfig, assetManager: AssetManager, resources: Resources) :
            this(config, assetManager, resources.getStringArray(config.labelId))

    init {
        this.tensorFlowInferenceInterface = TensorFlowInferenceInterface(assetManager, config.modelPath)
        this.labels = labels
        this.outputNames = arrayOf(config.outputName)
        this.output = FloatArray(config.numClasses)
    }

    abstract fun imagePreprocessing(bitmap: Bitmap): FloatArray

    /**
     * This func
     * @param bitmap raw bitmap to recognize
     */
    fun recognize(bitmap: Bitmap): Classification {
        val data = imagePreprocessing(bitmap)
        //feed data to tensorflow library
        tensorFlowInferenceInterface!!.feed(
                config.inputName,
                data,
                1,
                config.inputSize,
                config.inputSize,
                3
        )
        //get the possible outputs
        tensorFlowInferenceInterface!!.run(outputNames)
        //get the output
        tensorFlowInferenceInterface!!.fetch(config.outputName, output)
        return Classification(output!!, labels!!)
    }

}