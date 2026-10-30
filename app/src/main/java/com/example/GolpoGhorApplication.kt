package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class GolpoGhorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(this)
                } catch (e1: Exception) {
                    Log.w("GolpoGhorApplication", "Default FirebaseApp.initializeApp failed, initializing with options: ${e1.message}")
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:799928524815:android:a6624e059713b01ebe037a")
                        .setApiKey("AIzaSyB5tFXNvGK-vrpDrNsCXUJJbPY4PPCZs84")
                        .setProjectId("gen-lang-client-0318913908")
                        .setStorageBucket("gen-lang-client-0318913908.firebasestorage.app")
                        .setGcmSenderId("799928524815")
                        .build()
                    FirebaseApp.initializeApp(this, options)
                }
            }
            Log.i("GolpoGhorApplication", "FirebaseApp initialized successfully")
        } catch (e: Exception) {
            Log.e("GolpoGhorApplication", "Error initializing FirebaseApp: ${e.message}", e)
        }
    }
}
