package com.clinica.app

import com.clinica.data.DatabaseFactory
import com.clinica.data.databaseModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.module

object AppInitializer {
    private var isInitialized = false

    fun init(factory: DatabaseFactory): KoinApplication? {
        return if (!isInitialized) {
            isInitialized = true
            startKoin {
                modules(
                    databaseModule(factory)
                )
            }
        } else {
            null
        }
    }
}
