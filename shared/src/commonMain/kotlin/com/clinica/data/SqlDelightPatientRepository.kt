package com.clinica.data

import app.cash.sqldelight.coroutines.asFlow
import com.clinica.domain.model.Patient
import com.clinica.domain.repository.PatientRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SqlDelightPatientRepository(
    private val db: PsychNotesDb
) : PatientRepository {

    private val queries get() = db.patientsQueries

    override suspend fun upsert(patient: Patient) = withContext(Dispatchers.Default) {
        queries.insertPatient(
            id = patient.id,
            display_name = patient.displayName,
            first_name = patient.firstName,
            last_name = patient.lastName,
            dni = patient.dni,
            gender = patient.gender,
            birth_date = DateTimeMapper.localDateToString(patient.birthDate),
            phone = patient.phone,
            address = patient.address,
            created_at = DateTimeMapper.instantToString(patient.createdAt),
            updated_at = DateTimeMapper.instantToString(patient.updatedAt)
        )
    }

    override suspend fun getById(id: String): Patient? = withContext(Dispatchers.Default) {
        queries.selectPatientById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun getAllPatients(): List<Patient> = withContext(Dispatchers.Default) {
        queries.selectAllPatients().executeAsList().map { it.toDomain() }
    }

    override fun observeAll(): Flow<List<Patient>> =
        queries.selectAllPatients()
            .asFlow()
            .map { query -> query.executeAsList().map { it.toDomain() } }

    override suspend fun delete(id: String) = withContext(Dispatchers.Default) {
        queries.deletePatient(id)
    }

    override suspend fun deletePatient(patientId: String) = withContext(Dispatchers.Default) {
        queries.deletePatient(patientId)
    }
}
