package com.rico.omarw.rutasuruapan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.rico.omarw.rutasuruapan.Constants.PreferenceKeys
import com.rico.omarw.rutasuruapan.Constants.PreferenceKeys.DONATION
import com.rico.omarw.rutasuruapan.Constants.PreferenceKeys.PRIVACY_POLICY
import com.rico.omarw.rutasuruapan.Constants.PreferenceKeys.RATE
import com.rico.omarw.rutasuruapan.Constants.PreferenceKeys.SHOW_DIALOGS
import com.rico.omarw.rutasuruapan.Constants.PreferenceKeys.SOURCE_CODE
import com.rico.omarw.rutasuruapan.Constants.PreferenceKeys.VERSION
import com.rico.omarw.rutasuruapan.Constants.PreferenceKeys.WALK_DIST_LIMIT
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        val settingsContainer = findViewById<android.view.View>(R.id.settings)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_activity_root)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            toolbar.updatePadding(top = systemBars.top)
            settingsContainer.updatePadding(bottom = systemBars.bottom)
            insets
        }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.elevation = 10f

    }

    class SettingsFragment : PreferenceFragmentCompat(), Preference.SummaryProvider<ListPreference>,
        Preference.OnPreferenceClickListener {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            findPreference<ListPreference>(WALK_DIST_LIMIT)?.summaryProvider = this
            findPreference<Preference>(SHOW_DIALOGS)?.onPreferenceClickListener = this
            findPreference<Preference>(DONATION)?.onPreferenceClickListener = this
            findPreference<Preference>(RATE)?.onPreferenceClickListener = this
            findPreference<Preference>(PRIVACY_POLICY)?.onPreferenceClickListener = this
            findPreference<Preference>(SOURCE_CODE)?.onPreferenceClickListener = this

            preferenceScreen.addPreference(Preference(requireContext()).apply {
                isEnabled = false
                summary = BuildConfig.VERSION_NAME
                key = VERSION
            })
        }

        private fun deleteSomePreferences(c: Context) {
            PreferenceManager.getDefaultSharedPreferences(c).edit {
                remove(PreferenceKeys.HOW_TO_SHOW_ROUTE_DIALOG_SHOWN)
                remove(PreferenceKeys.REMOVE_MARKER_DIALOG_SHOWN)
                remove(PreferenceKeys.DISCLAIMER_SHOWN)
            }
        }

        private fun openLink(link: String) {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = link.toUri()
            })
        }

        override fun onPreferenceClick(preference: Preference): Boolean {
            when (preference.key) {
                SHOW_DIALOGS -> deleteSomePreferences(preference.context)
                DONATION -> openLink(getString(R.string.donation_link))
                RATE -> openLink("https://play.google.com/store/apps/details?id=${preference.context.packageName}")
                PRIVACY_POLICY -> openLink("https://github.com/bolderoctopus/uruapan-rutas-urbanos/blob/master/privacy%20policy.md")
                SOURCE_CODE -> openLink("https://github.com/bolderoctopus/uruapan-rutas-urbanos")
                else -> return false
            }
            return true
        }

        override fun provideSummary(preference: ListPreference): CharSequence {
            return getString(R.string.preference_summary_limit_distance, preference.entry)
        }
    }
}