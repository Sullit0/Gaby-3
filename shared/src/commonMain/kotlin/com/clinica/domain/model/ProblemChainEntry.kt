package com.clinica.domain.model

/**
 * Registro Pn dentro del análisis en cadena.
 */
data class ProblemChainEntry(
    val id: Long,
    val sessionId: String,
    val label: String,
    val vulnerabilidades: String?,
    val eventoDesencadenante: String?,
    val eslabones: String?,
    val problemasConducta: String?,
    val consecuentes: String?
)
