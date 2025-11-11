package com.safetravel.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/*
 * This is the main entry point for our app.
 * The @HiltAndroidApp annotation tells Hilt to start
 * its code generation process from this class.
 */
@HiltAndroidApp
class SafeTravelApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // You can put other app-wide initialization code here
    }
}