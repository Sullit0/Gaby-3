package com.clinica.android

import android.app.Application
import com.clinica.data.AndroidDatabaseFactory
import com.clinica.data.databaseModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PsychNotesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeKoin()
    }

    private fun initializeKoin() {
        startKoin {
            androidContext(this@PsychNotesApplication)
            modules(databaseModule(AndroidDatabaseFactory(this@PsychNotesApplication)))
        }
    }
}