package com.clinica.data

import com.clinica.domain.model.Patient
import kotlinx.datetime.Instant

internal fun Patients.toDomain(): Patient = Patient(
    id = id,
    displayName = display_name,
    firstName = first_name,
    lastName = last_name,
    dni = dni,
    gender = gender,
    birthDate = DateTimeMapper.stringToLocalDate(birth_date),
    phone = phone,
    address = address,
    createdAt = DateTimeMapper.stringToInstant(created_at) ?: Instant.parse(created_at),
    updatedAt = DateTimeMapper.stringToInstant(updated_at) ?: Instant.parse(updated_at)
)

internal data class PatientEntityParams(
    val id: String,
    val display_name: String,
    val first_name: String?,
    val last_name: String?,
    val dni: String?,
    val gender: String?,
    val birth_date: String?,
    val phone: String?,
    val address: String?,
    val created_at: String,
    val updated_at: String
)

internal fun Patient.toEntityParams(): PatientEntityParams = PatientEntityParams(
    id = id,
    display_name = displayName,
    first_name = firstName,
    last_name = lastName,
    dni = dni,
    gender = gender,
    birth_date = DateTimeMapper.localDateToString(birthDate),
    phone = phone,
    address = address,
    created_at = DateTimeMapper.instantToString(createdAt),
    updated_at = DateTimeMapper.instantToString(updatedAt)
)
