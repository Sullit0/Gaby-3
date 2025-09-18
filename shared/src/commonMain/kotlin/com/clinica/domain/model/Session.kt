package com.clinica.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Datos generales de una sesión/ficha.
 */
data class Session(
    val id: String,
    val patientId: String,
    val sessionCode: Int,
    val sessionDate: LocalDate?,
    val firstAttentionDate: LocalDate?,
    val motivoPrincipal: String?,
    val otrosMotivos: String?,
    val familyNotes: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
