package helfi2012.androidvision.utils

import android.graphics.Bitmap
import android.graphics.Color

object ImageUtils {
    val COLOR_IN_RGB = 255
    val RGB_CHANNELS = 3

    /**
     * Converting bitmap to float array
     */
    fun getFloatPixelData(bitmap: Bitmap): FloatArray {
        val bytePixels = getBytePixelData(bitmap)
        return FloatArray(bytePixels.size, { bytePixels[it].toFloat() / COLOR_IN_RGB.toFloat() })
    }

    fun getBytePixelData(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val retPixels = ByteArray(width * height * RGB_CHANNELS)

        var i = 0
        for (intColor in pixels) {
            retPixels[i] = Color.red(intColor).toByte()
            retPixels[i+1] = Color.blue(intColor).toByte()
            retPixels[i+2] = Color.green(intColor).toByte()
            i += 3
        }
        return retPixels
    }
}
