package com.clinica.domain.model

/**
 * Lista de metas asociadas a los problemas principales.
 */
data class ProblemGoals(
    val sessionId: String,
    val metas: String?
)
