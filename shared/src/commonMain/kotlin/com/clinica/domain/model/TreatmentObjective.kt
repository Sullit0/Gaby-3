package com.clinica.domain.model

data class TreatmentObjective(
    val id: Long,
    val sessionId: String,
    val stage: Stage,
    val field: String,
    val value: String?
) {
    enum class Stage {
        ETAPA_1,
        ETAPA_2,
        ETAPA_3,
        SECUNDARIOS
    }
}
