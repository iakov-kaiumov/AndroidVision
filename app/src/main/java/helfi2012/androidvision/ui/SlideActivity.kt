package helfi2012.androidvision.ui

import android.Manifest
import android.app.Fragment
import android.app.FragmentManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.preference.PreferenceManager
import android.support.v13.app.FragmentStatePagerAdapter
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import android.support.v4.view.ViewPager
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import helfi2012.androidvision.R


class SlideActivity : FragmentActivity() {

    private var viewPager: ViewPager? = null

    private var cameraFragment: CameraFragment? = null

    private var permissionGranted = false

    private fun requestCameraPermission(): Boolean {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE)
            return false
        }
        return true
    }

    private fun setPolice() {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            val builder = StrictMode.VmPolicy.Builder()
            StrictMode.setVmPolicy(builder.build())
            builder.detectFileUriExposure()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionGranted = true
                    cameraFragment!!.createCameraPreview()
                } else {
                    Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenceManager.setDefaultValues(this, R.xml.pref1, false)
        permissionGranted = requestCameraPermission()
        setPolice()
        initViews()
    }

    override fun onBackPressed() {
        if (viewPager!!.currentItem == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed()
        } else {
            // Otherwise, select the previous step.
            viewPager!!.currentItem = viewPager!!.currentItem - 1
        }
    }

    private fun initViews() {
        setContentView(R.layout.activity_slide)
        if (actionBar != null) {
            actionBar!!.hide()
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        // Instantiate a ViewPager and a PagerAdapter.
        viewPager = findViewById(R.id.pager) as ViewPager
        val mPagerAdapter = ScreenSlidePagerAdapter(fragmentManager)
        viewPager!!.adapter = mPagerAdapter
        viewPager!!.setPageTransformer(true, ZoomOutPageTransformer())
    }

    private inner class ScreenSlidePagerAdapter internal constructor(fragmentManager: FragmentManager) : FragmentStatePagerAdapter(fragmentManager) {

        override fun getItem(position: Int): Fragment? = when (position) {
            0 -> {
                cameraFragment = CameraFragment()
                if (permissionGranted) {
                    cameraFragment!!.createCameraPreview()
                }
                cameraFragment
            }
            1 -> SettingsFragment()
            else -> null
        }

        override fun getCount(): Int = NUM_PAGES
    }

    private class ZoomOutPageTransformer : ViewPager.PageTransformer {

        companion object {
            private val MIN_SCALE = 0.85f
            private val MIN_ALPHA = 0.5f
        }

        override fun transformPage(view: View, position: Float) {
            val pageWidth = view.width
            val pageHeight = view.height

            when {
                position < -1 ->
                    view.alpha = 0f
                position <= 1 -> {
                    val scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position))
                    val verticalMargin = pageHeight * (1 - scaleFactor) / 2
                    val horizontalMargin = pageWidth * (1 - scaleFactor) / 2
                    if (position < 0) {
                        view.translationX = horizontalMargin - verticalMargin / 2
                    } else {
                        view.translationX = -horizontalMargin + verticalMargin / 2
                    }

                    view.scaleX = scaleFactor
                    view.scaleY = scaleFactor
                    view.alpha = MIN_ALPHA + (scaleFactor - MIN_SCALE) / (1 - MIN_SCALE) * (1 - MIN_ALPHA)

                }
                else -> view.alpha = 0f
            }
        }
    }

    companion object {
        /**
         * The number of pages (wizard steps) to show in this demo.
         */
        private val NUM_PAGES = 2

        private val PERMISSION_REQUEST_CODE = 100
    }
}
