package com.clinica.domain.model

data class EvolutionNote(
    val id: Long,
    val sessionId: String,
    val titulo: String,
    val notaFecha: String?,
    val comportamientoTrabajado: String?,
    val apuntes: String?
)
