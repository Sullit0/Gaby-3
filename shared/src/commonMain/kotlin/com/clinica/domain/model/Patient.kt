package com.clinica.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Representa información básica del paciente.
 */
data class Patient(
    val id: String,
    val displayName: String,
    val firstName: String?,
    val lastName: String?,
    val dni: String?,
    val gender: String?,
    val birthDate: LocalDate?,
    val phone: String?,
    val address: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
