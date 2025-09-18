package com.clinica.app.form

import com.clinica.domain.model.TreatmentObjective

object SessionFormMetadata {
    val problemChainLabels = listOf("P1", "P2", "P3", "P4")

    val treatmentFields = listOf(
        TreatmentFieldDefinition(TreatmentObjective.Stage.ETAPA_1, "conductas_amenazan_vida", "Conductas que amenazan la vida"),
        TreatmentFieldDefinition(TreatmentObjective.Stage.ETAPA_1, "conductas_interfieren_en_terapia", "Conductas que interfieren durante la terapia"),
        TreatmentFieldDefinition(TreatmentObjective.Stage.ETAPA_1, "conductas_interfieren_calidad_vida", "Conductas que interfieren con la calidad de vida"),
        TreatmentFieldDefinition(TreatmentObjective.Stage.ETAPA_1, "deficit_habilidades", "Déficit de habilidades"),

        TreatmentFieldDefinition(TreatmentObjective.Stage.ETAPA_2, "disminuir_evitacion_experiencial", "Disminuir la evitación experiencial"),
        TreatmentFieldDefinition(TreatmentObjective.Stage.ETAPA_2, "aumentar_procesamiento_emocional", "Aumentar el procesamiento emocional"),
        TreatmentFieldDefinition(TreatmentObjective.Stage.ETAPA_2, "incrementar_recuperacion_emocional", "Incrementar la recuperación después de la experiencia emocional"),
        TreatmentFieldDefinition(TreatmentObjective.Stage.ETAPA_2, "disminuir_sensacion_vacio", "Disminuir la sensación de vacío"),
        TreatmentFieldDefinition(TreatmentObjective.Stage.ETAPA_2, "disminuir_alienacion", "Disminuir los sentimientos de alienación"),

        TreatmentFieldDefinition(TreatmentObjective.Stage.ETAPA_3, "resolucion_problemas", "Resolución de problemas"),
        TreatmentFieldDefinition(TreatmentObjective.Stage.ETAPA_3, "logro_metas", "Logro de metas"),
        TreatmentFieldDefinition(TreatmentObjective.Stage.ETAPA_3, "generalizacion_habilidades", "Generalización de habilidades"),

        TreatmentFieldDefinition(TreatmentObjective.Stage.SECUNDARIOS, "conductas_generadoras_crisis", "Conductas generadoras de crisis"),
        TreatmentFieldDefinition(TreatmentObjective.Stage.SECUNDARIOS, "vulnerabilidad_emocional", "Vulnerabilidad emocional"),
        TreatmentFieldDefinition(TreatmentObjective.Stage.SECUNDARIOS, "pasividad_activa", "Pasividad activa"),
        TreatmentFieldDefinition(TreatmentObjective.Stage.SECUNDARIOS, "inhibicion_emocional", "Inhibición emocional"),
        TreatmentFieldDefinition(TreatmentObjective.Stage.SECUNDARIOS, "auto_invalidacion", "Auto invalidación"),
        TreatmentFieldDefinition(TreatmentObjective.Stage.SECUNDARIOS, "competencia_aparente", "Competencia aparente")
    )

    val problemAnalysisNumbers = listOf(1, 2)

    val problemAnalysisFields = listOf(
        ProblemAnalysisFieldDefinition("comportamiento", "Comportamiento problema (DFI)"),
        ProblemAnalysisFieldDefinition("analisisSolucion", "Análisis de la solución"),
        ProblemAnalysisFieldDefinition("vulnerabilidad", "Vulnerabilidad"),
        ProblemAnalysisFieldDefinition("eventoExterno", "Evento precipitante externo"),
        ProblemAnalysisFieldDefinition("pensamientos", "Pensamientos"),
        ProblemAnalysisFieldDefinition("sensaciones", "Sensaciones"),
        ProblemAnalysisFieldDefinition("impulsos", "Impulsos"),
        ProblemAnalysisFieldDefinition("emociones", "Emociones"),
        ProblemAnalysisFieldDefinition("consecuenciasInmediatas", "Consecuencias inmediatas reforzantes"),
        ProblemAnalysisFieldDefinition("consecuenciasDemoradas", "Consecuencias demoradas"),
        ProblemAnalysisFieldDefinition("planCrisis", "Resuma el plan de crisis")
    )

    data class TreatmentFieldDefinition(
        val stage: TreatmentObjective.Stage,
        val field: String,
        val label: String
    )

    data class ProblemAnalysisFieldDefinition(
        val field: String,
        val label: String
    )
}
