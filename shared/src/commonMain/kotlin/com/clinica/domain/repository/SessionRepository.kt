package com.clinica.domain.repository

import com.clinica.domain.model.*
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    suspend fun createSession(patientId: String): Session
    suspend fun getSession(id: String): Session?
    suspend fun getAllSessions(): List<Session>
    fun observeSessions(patientId: String): Flow<List<Session>>
    suspend fun updateSession(session: Session)
    suspend fun deleteSession(sessionId: String)

    suspend fun upsertProblemChains(entries: List<ProblemChainEntry>)
    suspend fun getProblemChains(sessionId: String): List<ProblemChainEntry>

    suspend fun upsertProblemGoals(goals: ProblemGoals)
    suspend fun getProblemGoals(sessionId: String): ProblemGoals?

    suspend fun upsertPsychometrics(data: PsychometricData)
    suspend fun getPsychometrics(sessionId: String): PsychometricData?

    suspend fun upsertDysregulation(data: DysregulationAreas)
    suspend fun getDysregulation(sessionId: String): DysregulationAreas?

    suspend fun upsertBiosocial(data: BiosocialModel)
    suspend fun getBiosocial(sessionId: String): BiosocialModel?

    suspend fun upsertTreatmentObjectives(objectives: List<TreatmentObjective>)
    suspend fun getTreatmentObjectives(sessionId: String): List<TreatmentObjective>

    suspend fun upsertProblemAnalyses(analyses: List<ProblemAnalysis>)
    suspend fun getProblemAnalyses(sessionId: String): List<ProblemAnalysis>

    suspend fun upsertEvolutionNotes(notes: List<EvolutionNote>)
    suspend fun getEvolutionNotes(sessionId: String): List<EvolutionNote>

    suspend fun upsertTasks(tasks: SessionTasks)
    suspend fun getTasks(sessionId: String): SessionTasks?

    suspend fun addAttachment(attachment: Attachment)
    suspend fun getAttachments(sessionId: String): List<Attachment>
    suspend fun removeAttachment(attachmentId: String)

    suspend fun appendHistory(entry: SessionHistoryEntry)
    suspend fun getHistory(sessionId: String): List<SessionHistoryEntry>
}
