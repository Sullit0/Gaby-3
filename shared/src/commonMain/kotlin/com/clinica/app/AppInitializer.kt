package com.clinica.app

import com.clinica.data.DatabaseFactory
import com.clinica.data.databaseModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.module

object AppInitializer {
    fun init(factory: DatabaseFactory): KoinApplication =
        startKoin {
            modules(
                databaseModule(factory)
            )
        }
}
