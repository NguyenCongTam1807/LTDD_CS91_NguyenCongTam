package com.theapache64.removebgexample.preferences

import android.content.Context

class AppPreferences constructor(context: Context) {
    private var sharedPreferences = context.getSharedPreferences("APP_PREFERENCES", Context.MODE_PRIVATE)

    fun setViLocale(enable: Boolean){
        sharedPreferences!!
            .edit()
            .putBoolean("ViLocale", enable)
            .apply()
    }
    fun isUsingViLocale(): Boolean {
        return sharedPreferences!!.getBoolean("ViLocale", false)
    }

    fun setDarkMode(enable: Boolean){
        sharedPreferences!!
            .edit()
            .putBoolean("darkMode", enable)
            .apply()
    }
    fun isInDarkMode(): Boolean {
        return sharedPreferences!!.getBoolean("darkMode", false)
    }

    fun setFolderMode(enable: Boolean){
        sharedPreferences!!
            .edit()
            .putBoolean("folderMode", enable)
            .apply()
    }

    fun isInFolderMode(): Boolean {
        return sharedPreferences!!.getBoolean("folderMode", false)
    }
}