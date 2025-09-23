package com.clinica.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

class AndroidDatabaseFactory(
    private val context: Context? = null
) : DatabaseFactory {

    override fun createDriver(): SqlDriver {
        val appContext = context ?: throw IllegalStateException("Context not provided")
        return AndroidSqliteDriver(
            schema = PsychNotesDb.Schema,
            context = appContext,
            name = "psych_notes.db"
        )
    }
}