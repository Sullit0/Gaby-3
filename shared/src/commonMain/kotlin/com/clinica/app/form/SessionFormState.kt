package com.clinica.app.form

import com.clinica.domain.model.Attachment
import com.clinica.domain.model.BiosocialModel
import com.clinica.domain.model.DysregulationAreas
import com.clinica.domain.model.EvolutionNote
import com.clinica.domain.model.Patient
import com.clinica.domain.model.ProblemAnalysis
import com.clinica.domain.model.ProblemChainEntry
import com.clinica.domain.model.ProblemGoals
import com.clinica.domain.model.PsychometricData
import com.clinica.domain.model.Session
import com.clinica.domain.model.SessionHistoryEntry
import com.clinica.domain.model.SessionTasks
import com.clinica.domain.model.TreatmentObjective

data class SessionFormState(
    val patient: Patient? = null,
    val session: Session? = null,
    val tasks: SessionTasks? = null,
    val attachments: List<Attachment> = emptyList(),
    val familyNotes: String = "",
    val problemChains: List<ProblemChainEntry> = emptyList(),
    val problemGoals: ProblemGoals? = null,
    val psychometrics: PsychometricData? = null,
    val dysregulation: DysregulationAreas? = null,
    val biosocial: BiosocialModel? = null,
    val treatmentObjectives: List<TreatmentObjective> = emptyList(),
    val problemAnalyses: List<ProblemAnalysis> = emptyList(),
    val evolutionNotes: List<EvolutionNote> = emptyList(),
    val history: List<SessionHistoryEntry> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
