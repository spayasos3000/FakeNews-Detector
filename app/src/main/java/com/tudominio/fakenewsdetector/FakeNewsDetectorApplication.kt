package com.tudominio.fakenewsdetector

import android.app.Application
import com.google.firebase.FirebaseApp

class FakeNewsDetectorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
