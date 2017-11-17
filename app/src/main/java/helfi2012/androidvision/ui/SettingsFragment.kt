package helfi2012.androidvision.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment
import android.preference.Preference
import android.preference.PreferenceFragment
import android.support.v4.app.ActivityCompat
import android.widget.Toast
import helfi2012.androidvision.DownloadService
import helfi2012.androidvision.R
import helfi2012.androidvision.VisionApp
import java.io.File
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View


class SettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        val KEY_PREF_USE_INTERNET = "use_internet"
        val KEY_PREF_DOWNLOAD_NETWORK = "download"
        val NETWORK_FILE_PATH = Environment.getExternalStorageDirectory().absolutePath + "/androidVision/model.pb"
        val DOWNLOAD_LINK = ""
    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view!!.setBackgroundColor(ActivityCompat.getColor(activity, android.R.color.white))
        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.pref1)
        val preference = findPreference(KEY_PREF_DOWNLOAD_NETWORK) as Preference
        preference.setOnPreferenceClickListener {
            System.out.println("ON PREF CLICK")
            if (File(NETWORK_FILE_PATH).exists()) {
                Toast.makeText(activity, getString(R.string.error_file_exist), Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(activity, DownloadService::class.java)
                intent.putExtra(DownloadService.KEY_LINK, DOWNLOAD_LINK)
                intent.putExtra(DownloadService.KEY_FILE_NAME, NETWORK_FILE_PATH)
                activity.startService(intent)
            }
            return@setOnPreferenceClickListener true
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        (activity.application as VisionApp).onPreferencesChange()
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }
}
