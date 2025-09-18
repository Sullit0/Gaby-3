package com.clinica.domain.model

data class BiosocialModel(
    val sessionId: String,
    val vulnerabilidadEmocional: String?,
    val sensibilidad: String?,
    val intensidad: String?,
    val lentoRetornoCalma: String?,
    val invalidacionAmbiental: String?,
    val criticarEmociones: String?,
    val otros: String?
)
