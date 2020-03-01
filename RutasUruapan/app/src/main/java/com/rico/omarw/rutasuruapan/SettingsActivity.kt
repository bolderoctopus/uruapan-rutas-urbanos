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
            findPreference<ListPreference>("walk_dist_limit")?.summaryProvider = this
            findPreference<Preference>("show_dialogs_again")?.onPreferenceClickListener = this
            findPreference<Preference>("donation")?.onPreferenceClickListener = this
            findPreference<Preference>("rate")?.onPreferenceClickListener = this
        }

        private fun deleteSomePreferences(c: Context){
            val editor = PreferenceManager.getDefaultSharedPreferences(c).edit()
            editor.remove("has_inf_dialog1_been_shown")
            editor.remove("has_inf_dialog2_been_shown")
            editor.remove("has_disclaimer_been_shown")
            editor.apply()
        }

        private fun openLink(link: String){
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(link)
            })
        }

        override fun onPreferenceClick(preference: Preference?): Boolean {
            when (preference?.key) {
                "show_dialogs_again" -> deleteSomePreferences(preference.context)
                "donation" -> openLink(getString(R.string.donation_link))
                "rate" -> openLink("https://play.google.com/store/apps/details?id=${preference.context.packageName}")
                else -> return false
            }
            return true
        }

        override fun provideSummary(preference: ListPreference?): CharSequence {
            return if(preference != null)
                getString(R.string.preference_summary_limit_distance, preference.entry)
            else ""
        }
    }
}