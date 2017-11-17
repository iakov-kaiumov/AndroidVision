package helfi2012.androidvision.ui

import android.annotation.SuppressLint
import android.app.Fragment
import android.graphics.Color
import android.os.Bundle
import android.support.v13.app.ActivityCompat
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import com.plattysoft.leonids.ParticleSystem
import helfi2012.androidvision.R
import helfi2012.androidvision.VisionApp
import helfi2012.androidvision.recognision.CameraPreviewCallback
import helfi2012.androidvision.utils.ImageUtils
import helfi2012.androidvision.views.CameraPreview
import java.util.*


class CameraFragment : Fragment(), CameraPreviewCallback.NetworkListener {

    companion object {
        private val GOOGLE_URL = "https://www.google.ru/search?q="
        private val DELAY = 5
        private val TIMER_TICK = 1000.toLong()
        private val ANIMATION_DURATION = 400.toLong()
        private enum class WebBarState {
            INVISIBLE,
            TEXT_VIEW,
            WEB_VIEW
        }
    }

    private var cameraPreviewCallBack: CameraPreviewCallback? = null

    private var currentWebBarState: WebBarState = WebBarState.INVISIBLE

    private var scoreTextView: TextView? = null
    private var fpsTextView: TextView? = null
    private var labelTextView: TextView? = null
    private var cameraPreview: CameraPreview? = null
    private var networkProgressBar: ProgressBar? = null
    private var webProgressBar: ProgressBar? = null
    private var cameraLayout: RelativeLayout? = null
    private var webLayout: LinearLayout? = null
    private var webView: WebView? = null
    private var webButton: Button? = null

    private var foodLabel: String? = null

    private var timer = Timer()
    private var seconds = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layout = inflater.inflate(R.layout.fragment_camera, container, false)
        initViews(layout)
        return layout
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraPreview!!.stop()
        cameraLayout!!.removeView(cameraPreview)
        cameraPreview = null
    }

    private fun initViews(layout: View) {

        networkProgressBar = layout.findViewById(R.id.networkProgressBar) as ProgressBar
        webProgressBar = layout.findViewById(R.id.webViewProgressBar) as ProgressBar
        labelTextView = layout.findViewById(R.id.foodLabelTextView) as TextView
        fpsTextView = layout.findViewById(R.id.FPSTextView) as TextView
        scoreTextView = layout.findViewById(R.id.ScoreTextView) as TextView
        cameraLayout = layout.findViewById(R.id.preview) as RelativeLayout
        webLayout = layout.findViewById(R.id.webLayout) as LinearLayout

        webView = layout.findViewById(R.id.webView) as WebView
        @SuppressLint("SetJavaScriptEnabled")
        webView!!.settings.javaScriptEnabled = true
        webView!!.settings.builtInZoomControls = true

        webView!!.setWebChromeClient(object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, progress: Int) {
                webProgressBar!!.progress = progress
                if (progress == 100) {
                    webProgressBar!!.visibility = ProgressBar.GONE
                }
            }
        })

        webView!!.setWebViewClient(object : WebViewClient() {
            @Suppress("OverridingDeprecatedMember")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = false
        })

        webButton = layout.findViewById(R.id.button) as Button
        webButton!!.setOnClickListener {
            when (currentWebBarState) {
                WebBarState.TEXT_VIEW -> {
                    if (foodLabel == null) return@setOnClickListener
                    timer.cancel()
                    webProgressBar!!.visibility = ProgressBar.VISIBLE
                    webView!!.loadUrl(GOOGLE_URL + foodLabel)
                    changeWebBarState(WebBarState.WEB_VIEW)
                    webButton!!.text = getString(R.string.close_label)
                    scoreTextView!!.visibility = TextView.INVISIBLE
                    cameraPreviewCallBack!!.disableNetworks()
                }
                WebBarState.WEB_VIEW -> {
                    scoreTextView!!.visibility = TextView.VISIBLE
                    cameraPreviewCallBack!!.enableNetworks()
                    webButton!!.text = getString(R.string.open_label)
                    changeWebBarState(WebBarState.TEXT_VIEW)
                    startTimer()
                }
                else -> {}
            }
        }

    }

    fun createCameraPreview() {
        cameraPreviewCallBack = CameraPreviewCallback(activity.assets, resources,
                (activity.application as VisionApp).socketConnection!!, this)
        cameraPreview = CameraPreview(activity, cameraPreviewCallBack,
                CameraPreview.CameraMode.Back, CameraPreview.LayoutMode.FitToParent)
        val previewLayoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        cameraLayout!!.addView(cameraPreview, 0, previewLayoutParams)
        //Bringing Views to front after adding CameraPreview
        fpsTextView!!.bringToFront()
        scoreTextView!!.bringToFront()
        networkProgressBar!!.bringToFront()
    }

    private fun changeWebBarState(webBarState: WebBarState) {
        if (webBarState == currentWebBarState) return
        val displayMetrics = this.resources.displayMetrics
        val dpHeight = displayMetrics.heightPixels / displayMetrics.density
        when (webBarState) {
            WebBarState.INVISIBLE -> {
                val startHeight = if (currentWebBarState == WebBarState.TEXT_VIEW) 40f else dpHeight
                com.github.florent37.viewanimator.ViewAnimator.animate(webLayout)
                        .dp().height(startHeight, 0f)
                        .duration(ANIMATION_DURATION)
                        .start()
            }
            WebBarState.TEXT_VIEW -> {
                val startHeight = if (currentWebBarState == WebBarState.INVISIBLE) 0f else dpHeight
                com.github.florent37.viewanimator.ViewAnimator.animate(webLayout)
                        .dp().height(startHeight, 40f)
                        .duration(ANIMATION_DURATION)
                        .start()
            }
            WebBarState.WEB_VIEW -> {
                val startHeight = if (currentWebBarState == WebBarState.INVISIBLE) 0f else 40f
                com.github.florent37.viewanimator.ViewAnimator.animate(webLayout)
                        .dp().height(startHeight, dpHeight)
                        .duration(ANIMATION_DURATION)
                        .start()

            }
        }
        currentWebBarState = webBarState
    }

    private fun createFirework() {
        val ps = ParticleSystem(activity, 50, R.drawable.star_pink, 600)
        ps.setScaleRange(0.7f, 1.3f)
        ps.setSpeedRange(0.1f, 0.25f)
        ps.setRotationSpeedRange(90f, 180f)
        ps.setFadeOut(200, AccelerateInterpolator())
        ps.oneShot(fpsTextView, 70)

        val ps2 = ParticleSystem(activity, 50, R.drawable.star_white, 600)
        ps2.setScaleRange(0.7f, 1.3f)
        ps2.setSpeedRange(0.1f, 0.25f)
        ps2.setRotationSpeedRange(90f, 180f)
        ps2.setFadeOut(200, AccelerateInterpolator())
        ps2.oneShot(fpsTextView, 70)
    }

    private fun setLabelText() {
        if (foodLabel == null) return
        //get colors from resources and convert it to HexString
        val textColor = String.format("#%06X", 0xFFFFFF and ActivityCompat.getColor(activity, R.color.textColor))
        val labelColor = String.format("#%06X", 0xFFFFFF and ActivityCompat.getColor(activity, R.color.colorAccent))
        //Create colored html text
        val text = "<font color=$textColor>${getString(R.string.predicting)}</font> <font color=$labelColor>$foodLabel</font>"
        @Suppress("DEPRECATION")
        labelTextView!!.text = Html.fromHtml(text)
    }

    private fun startTimer() {
        timer = Timer()
        seconds = 0
        timer.schedule(object : TimerTask() {
            override fun run() {
                seconds++
                if (seconds == DELAY) {
                    activity.runOnUiThread { changeWebBarState(WebBarState.INVISIBLE) }
                    seconds = 0
                    foodLabel = null
                    timer.cancel()
                }
            }
        }, 0, TIMER_TICK)
    }

    override fun onNetworkInitialized(name: String, successful: Boolean) {
        if (successful) {
            Toast.makeText(activity, getString(R.string.network_initialized), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(activity, "Error", Toast.LENGTH_LONG).show()
        }
    }

    override fun onNetworkRecognitionStart(name: String) {
        activity.runOnUiThread {
            when (name) {
                CameraPreviewCallback.MULTI_CLASS_CONFIG.name -> networkProgressBar!!.visibility = ProgressBar.VISIBLE
                CameraPreviewCallback.BINARY_CONFIG.name -> {
                }
                else -> {
                }
            }
        }
    }

    override fun onNetworkRecognitionStop(name: String, label: String, score: Float) {
        activity.runOnUiThread {
            when (name) {
                CameraPreviewCallback.MULTI_CLASS_CONFIG.name -> {
                    networkProgressBar!!.visibility = ProgressBar.INVISIBLE
                    createFirework()
                    foodLabel = label
                    if (currentWebBarState == WebBarState.WEB_VIEW) {
                        seconds = 0
                        timer.cancel()
                    } else {
                        setLabelText()
                        changeWebBarState(WebBarState.TEXT_VIEW)
                        startTimer()
                    }
                }
                CameraPreviewCallback.BINARY_CONFIG.name -> {
                    scoreTextView!!.text = (score * 100).toInt().toString().plus(getString(R.string.food_label))
                    val intScore = (score * ImageUtils.COLOR_IN_RGB).toInt()
                    val color = Color.rgb(ImageUtils.COLOR_IN_RGB - intScore, intScore, 0)
                    scoreTextView!!.setTextColor(color)
                }
                else -> {}
            }
        }
    }

    override fun onFrame(framesPerSecond: Int) {}
}
