package helfi2012.androidvision.recognition

import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.Bitmap
import helfi2012.androidvision.utils.ImageUtils

internal class Classifier(config: NetworkConfig, assetManager: AssetManager, resources: Resources):
        TensorFlowClassifier(config, assetManager, resources) {

    override fun imagePreprocessing(bitmap: Bitmap): FloatArray {
        val cropBitmap = Bitmap.createScaledBitmap(bitmap, config.inputSize.toInt(), config.inputSize.toInt(), false)
        return ImageUtils.getFloatPixelData(cropBitmap)
    }

}