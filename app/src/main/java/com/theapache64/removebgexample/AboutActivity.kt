package com.theapache64.removebgexample

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.theapache64.removebgexample.preferences.AppPreferences
import java.util.*

class AboutActivity : AppCompatActivity() {

    var appPreferences: AppPreferences? = null

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
        if (appPreferences!!.isInDarkMode()) {
            setTheme(R.style.DarkTheme)
        } else {
            setTheme(R.style.AppTheme)
        }
        setContentView(R.layout.activity_about)

        val toolbar : Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = resources.getString(R.string.about)
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
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
        startActivity(Intent(this@AboutActivity, MainActivity::class.java))
    }
}