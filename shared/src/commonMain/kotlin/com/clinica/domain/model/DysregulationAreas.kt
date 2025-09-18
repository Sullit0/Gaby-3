package com.clinica.domain.model

data class DysregulationAreas(
    val sessionId: String,
    val emocional: String?,
    val conductual: String?,
    val interpersonal: String?,
    val selfValores: String?,
    val cognitiva: String?,
    val resumen: String?,
    val bsl23Aplicado: Boolean
)
