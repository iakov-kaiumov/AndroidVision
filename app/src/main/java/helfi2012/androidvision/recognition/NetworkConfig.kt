package helfi2012.androidvision.recognition

/**
 * Neural network configuration class
 * @property name Neural network name
 * @property modelPath path to model in assets
 * @property labelId id of string-array in resources.
 * @property inputName model input name
 * @property outputName model output name
 * @property inputSize image size
 * @property numClasses number of output classes
 */
open class NetworkConfig(val name: String, val modelPath: String, val labelId: Int, val inputName: String,
                         val outputName: String, val inputSize: Long, val numClasses: Int) {
    constructor(name: String, modelPath: String, inputName: String,
                outputName: String, inputSize: Long, numClasses: Int):
            this(name, modelPath, -1, inputName, outputName, inputSize, numClasses)
}