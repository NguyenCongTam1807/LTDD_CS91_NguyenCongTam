package com.congtam.backgroundremover

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.preference.*
import com.congtam.backgroundremover.backend.RemoveBg


class SettingsFragment : PreferenceFragmentCompat(){
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        this.findPreference<EditTextPreference>("apiKey")?.setOnPreferenceChangeListener { _, newValue ->
            RemoveBg.init(newValue.toString())
            true
        }

        this.findPreference<CheckBoxPreference>("folderMode")?.setOnPreferenceChangeListener { _, newValue ->
            HomeFragment.folderMode = newValue as Boolean
            true
        }

        this.findPreference<Preference>("shortcut")?.setOnPreferenceClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.data = Uri.parse("https://www.remove.bg/dashboard#api-key")
            requireContext().startActivity(intent)
            true
        }
    }
}