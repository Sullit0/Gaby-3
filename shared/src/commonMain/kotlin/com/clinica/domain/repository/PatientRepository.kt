package com.clinica.domain.repository

import com.clinica.domain.model.Patient
import kotlinx.coroutines.flow.Flow

interface PatientRepository {
    suspend fun upsert(patient: Patient)
    suspend fun getById(id: String): Patient?
    fun observeAll(): Flow<List<Patient>>
    suspend fun delete(id: String)
}
