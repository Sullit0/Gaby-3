package com.clinica.data

import com.clinica.domain.repository.PatientRepository
import com.clinica.domain.repository.SessionRepository
import org.koin.core.module.Module
import org.koin.dsl.module

fun databaseModule(factory: DatabaseFactory): Module = module {
    single { factory.createDriver() }
    single { PsychNotesDb(get()) }
    single<PatientRepository> { SqlDelightPatientRepository(get()) }
    single<SessionRepository> { SqlDelightSessionRepository(get()) }
}
