package com.example.demo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Entry point for Hilt: triggers generation of the application-level
 * dependency container (SingletonComponent).
 */
@HiltAndroidApp
class DemoApplication : Application()
