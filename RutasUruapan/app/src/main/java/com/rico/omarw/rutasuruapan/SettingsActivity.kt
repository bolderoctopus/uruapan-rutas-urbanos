package com.rico.omarw.rutasuruapan

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
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

    class SettingsFragment : PreferenceFragmentCompat(), Preference.SummaryProvider<ListPreference> {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            findPreference<ListPreference>("walk_dist_limit")?.summaryProvider = this
            findPreference<Preference>("show_dialogs_again")?.setOnPreferenceClickListener {
                deleteSomePreferences(it.context)
                true
            }
        }

        fun deleteSomePreferences(c: Context){
            val editor = PreferenceManager.getDefaultSharedPreferences(c).edit()
            editor.remove("has_inf_dialog1_been_shown")
            editor.remove("has_inf_dialog2_been_shown")
            editor.remove("has_disclaimer_been_shown")
            editor.apply()
        }



        override fun provideSummary(preference: ListPreference?): CharSequence {
            return if(preference != null)
                getString(R.string.preference_summary_limit_distance, preference.entry)
            else ""
        }
    }
}