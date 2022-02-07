package com.congtam.backgroundremover

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.preference.PreferenceManager


class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navController = findNavController(R.id.nav_host_fragment)
        setupActionBarWithNavController(navController)
        val settings = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val darkTheme = settings.getBoolean("theme",false)
        val theme = if (darkTheme) R.style.Theme_BackgroundRemoverDark
        else  R.style.Theme_BackgroundRemover
        setTheme(theme)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()||super.onSupportNavigateUp()
    }
}