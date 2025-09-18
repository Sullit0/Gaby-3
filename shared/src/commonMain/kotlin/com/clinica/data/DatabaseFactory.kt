package com.clinica.data

import app.cash.sqldelight.db.SqlDriver

interface DatabaseFactory {
    fun createDriver(): SqlDriver
}
