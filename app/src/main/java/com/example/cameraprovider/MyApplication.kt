package com.example.cameraprovider

import android.app.Application
import android.content.Intent
import com.example.cameraprovider.notification.NotificationService
import com.google.firebase.FirebaseApp

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}