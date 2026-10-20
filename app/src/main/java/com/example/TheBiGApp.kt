package com.example

import android.app.Application
import com.example.data.local.TheBiGDatabase
import com.example.data.repository.TheBiGRepository

class TheBiGApp : Application() {
    val database: TheBiGDatabase by lazy { TheBiGDatabase.getDatabase(this) }
    val repository: TheBiGRepository by lazy { TheBiGRepository(database) }

    companion object {
        lateinit var instance: TheBiGApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
