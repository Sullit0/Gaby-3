package com.clinica.domain.model

data class ProblemAnalysis(
    val id: Long,
    val sessionId: String,
    val problemNumber: Int,
    val vulnerabilidad: String?,
    val eventoExterno: String?,
    val pensamientos: String?,
    val sensaciones: String?,
    val impulsos: String?,
    val emociones: String?,
    val consecuenciasInmediatas: String?,
    val consecuenciasDemoradas: String?,
    val planCrisis: String?,
    val analisisSolucion: String?
)
