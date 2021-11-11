package com.theapache64.bgremover

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import com.theapache64.bgremover.preferences.AppPreferences
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appPreferences = AppPreferences(this)

        //set language
        val lang = if (appPreferences!!.isUsingViLocale()) {
            "vi"
        }
        else "en"
        val config = resources.configuration
        val locale = Locale(lang)
        Locale.setDefault(locale)
        config!!.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        //set theme
        appPreferences = AppPreferences(this)
        if (appPreferences!!.isInDarkMode()) {
            setTheme(R.style.DarkTheme)
        } else {
            setTheme(R.style.AppTheme)
        }

        setContentView(R.layout.activity_settings)
        val toolbar : Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = resources.getString(R.string.settings)
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)

        // Language switch
        var languageSwitch: SwitchCompat = findViewById(R.id.language_switch)
        if (appPreferences.isUsingViLocale()) {
            languageSwitch.isChecked = true
        }
        languageSwitch.setOnCheckedChangeListener { _: CompoundButton?, _: Boolean ->
            setViLocale(!appPreferences.isUsingViLocale())
        }

        // Theme switch
        var themeSwitch: SwitchCompat = findViewById(R.id.theme_switch)
        if (appPreferences.isInDarkMode()) {
            themeSwitch.isChecked = true
        }
        themeSwitch.setOnCheckedChangeListener { _: CompoundButton?, _: Boolean ->
            setDarkMode(!appPreferences.isInDarkMode())
        }

        // Folder mode switch
        var folderSwitch: SwitchCompat = findViewById(R.id.folder_switch)
        if (appPreferences.isInFolderMode()) {
            folderSwitch.isChecked = true
        }
        folderSwitch.setOnCheckedChangeListener { _: CompoundButton?, _: Boolean ->
            setImagePickerFolderMode(!appPreferences.isInFolderMode())
        }
    }

    private fun setViLocale(enable: Boolean){
        appPreferences.setViLocale(enable)
        val intent = Intent(applicationContext, SettingsActivity::class.java)
        startActivity(intent)
        finish()
    }
    private fun setDarkMode(enable: Boolean) {
        appPreferences.setDarkMode(enable)
        val intent = Intent(applicationContext, SettingsActivity::class.java)
        startActivity(intent)
        finish()
    }
    private fun setImagePickerFolderMode(enable: Boolean) {
        appPreferences.setFolderMode(enable)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
        startActivity(Intent(this@SettingsActivity, MainActivity::class.java))
    }
}