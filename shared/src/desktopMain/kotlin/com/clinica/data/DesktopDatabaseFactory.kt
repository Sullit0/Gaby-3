package com.clinica.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.pathString
import kotlin.io.path.toPath
import com.clinica.data.PsychNotesDb

class DesktopDatabaseFactory(
    private val basePath: Path
) : DatabaseFactory {
    override fun createDriver(): SqlDriver {
        if (!Files.exists(basePath)) {
            basePath.createDirectories()
        }
        val dbPath = basePath.resolve("psych_notes.db")
        val jdbcUrl = "jdbc:sqlite:${dbPath.toAbsolutePath().pathString}"
        val driver = JdbcSqliteDriver(url = jdbcUrl)
        if (!dbPath.exists()) {
            PsychNotesDb.Schema.create(driver)
        }
        return driver
    }
}
