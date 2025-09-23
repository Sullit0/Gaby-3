package com.clinica.domain.repository

import com.clinica.domain.model.Patient
import kotlinx.coroutines.flow.Flow

interface PatientRepository {
    suspend fun upsert(patient: Patient)
    suspend fun getById(id: String): Patient?
    suspend fun getAllPatients(): List<Patient>
    fun observeAll(): Flow<List<Patient>>
    suspend fun delete(id: String)
    suspend fun deletePatient(patientId: String)
}
