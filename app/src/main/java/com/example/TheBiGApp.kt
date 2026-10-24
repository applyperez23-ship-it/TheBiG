package com.example

import android.app.Application
import com.example.data.local.TheBiGDatabase
import com.example.data.repository.TheBiGRepository

class TheBiGApp : Application() {

    lateinit var database: TheBiGDatabase
        private set

    lateinit var repository: TheBiGRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = TheBiGDatabase.getDatabase(this)
        repository = TheBiGRepository(database)
    }

    companion object {
        lateinit var instance: TheBiGApp
            private set
    }
}
