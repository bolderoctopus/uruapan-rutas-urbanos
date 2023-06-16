package com.rico.omarw.rutasuruapan

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.rico.omarw.rutasuruapan.Constants.PreferenceKeys

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.elevation = 10f

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat(), Preference.SummaryProvider<ListPreference>, Preference.OnPreferenceClickListener {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            findPreference<ListPreference>(PreferenceKeys.WALK_DIST_LIMIT)?.summaryProvider = this
            findPreference<Preference>("show_dialogs_again")?.onPreferenceClickListener = this
            findPreference<Preference>("donation")?.onPreferenceClickListener = this
            findPreference<Preference>("rate")?.onPreferenceClickListener = this
            findPreference<Preference>("privacy_policy")?.onPreferenceClickListener = this
            findPreference<Preference>("source_code")?.onPreferenceClickListener = this

            preferenceScreen.addPreference(Preference(requireContext()).apply {
                isEnabled = false
                summary = BuildConfig.VERSION_NAME
                key = "version"
            })
        }

        private fun deleteSomePreferences(c: Context){
            val editor = PreferenceManager.getDefaultSharedPreferences(c).edit()
            editor.remove(PreferenceKeys.DIALOG_1_SHOWN)
            editor.remove(PreferenceKeys.DIALOG_2_SHOWN)
            editor.remove(PreferenceKeys.DISCLAIMER_SHOWN)
            editor.apply()
        }

        private fun openLink(link: String){
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(link)
            })
        }

        override fun onPreferenceClick(preference: Preference): Boolean {
            when (preference.key) {
                "show_dialogs_again" -> deleteSomePreferences(preference.context)
                "donation" -> openLink(getString(R.string.donation_link))
                "rate" -> openLink("https://play.google.com/store/apps/details?id=${preference.context.packageName}")
                "privacy_policy" -> openLink("https://github.com/bolderoctopus/uruapan-rutas-urbanos/blob/master/privacy%20policy.md")
                "source_code" -> openLink("https://github.com/bolderoctopus/uruapan-rutas-urbanos")
                else -> return false
            }
            return true
        }

        override fun provideSummary(preference: ListPreference): CharSequence {
            return if(preference != null)
                getString(R.string.preference_summary_limit_distance, preference.entry)
            else ""
        }
    }
}