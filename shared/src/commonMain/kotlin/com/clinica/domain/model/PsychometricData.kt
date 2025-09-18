package com.clinica.domain.model

data class PsychometricData(
    val sessionId: String,
    val coeficienteValor: String?,
    val coeficienteClasificacion: String?,
    val temperamento: String?,
    val personalidad: String?,
    val atencion: String?,
    val problemasConducta: String?,
    val dinamicaFamiliar: String?,
    val otrosInteres: String?
)
