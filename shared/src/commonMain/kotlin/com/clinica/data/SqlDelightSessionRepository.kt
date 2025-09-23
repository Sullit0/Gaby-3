package com.clinica.data

import app.cash.sqldelight.coroutines.asFlow
import com.benasher44.uuid.uuid4
import com.clinica.domain.model.Attachment
import com.clinica.domain.model.BiosocialModel
import com.clinica.domain.model.DysregulationAreas
import com.clinica.domain.model.EvolutionNote
import com.clinica.domain.model.ProblemAnalysis
import com.clinica.domain.model.ProblemChainEntry
import com.clinica.domain.model.ProblemGoals
import com.clinica.domain.model.PsychometricData
import com.clinica.domain.model.Session
import com.clinica.domain.model.SessionHistoryEntry
import com.clinica.domain.model.SessionTasks
import com.clinica.domain.model.TreatmentObjective
import com.clinica.domain.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class SqlDelightSessionRepository(
    private val db: PsychNotesDb
) : SessionRepository {

    private val sessions get() = db.patientSessionsQueries
    private val chains get() = db.sessionProblemChainsQueries
    private val goals get() = db.sessionProblemGoalsQueries
    private val psychometrics get() = db.sessionPsychometricsQueries
    private val dysregulation get() = db.sessionDysregulationQueries
    private val biosocial get() = db.sessionBiosocialQueries
    private val objectives get() = db.sessionTreatmentObjectivesQueries
    private val analyses get() = db.sessionProblemAnalysisQueries
    private val evolution get() = db.sessionEvolutionNotesQueries
    private val tasks get() = db.sessionTasksQueries
    private val attachments get() = db.attachmentsQueries
    private val history get() = db.sessionHistoryQueries

    override suspend fun createSession(patientId: String): Session = withContext(Dispatchers.Default) {
        val now = Clock.System.now()
        val id = uuid4().toString()
        val maxCode = sessions.selectMaxSessionCode(patientId).executeAsOne()
        val nextCode = maxCode + 1
        sessions.insertSession(
            id = id,
            patient_id = patientId,
            session_code = nextCode,
            session_date = null,
            first_attention_date = null,
            motivo_principal = null,
            otros_motivos = null,
            created_at = DateTimeMapper.instantToString(now),
            updated_at = DateTimeMapper.instantToString(now),
            family_notes = null
        )
        sessions.selectSessionById(id).executeAsOne().toDomain()
    }

    override suspend fun getSession(id: String): Session? = withContext(Dispatchers.Default) {
        sessions.selectSessionById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun getAllSessions(): List<Session> = withContext(Dispatchers.Default) {
        // Obtener todos los pacientes primero y luego sus sesiones
        emptyList<Session>() // Temporal: simplificar para que compile
    }

    override fun observeSessions(patientId: String): Flow<List<Session>> =
        sessions.selectSessionsByPatient(patientId)
            .asFlow()
            .map { query -> query.executeAsList().map { it.toDomain() } }

    override suspend fun updateSession(session: Session) = withContext(Dispatchers.Default) {
        sessions.updateSession(
            session_code = session.sessionCode.toLong(),
            session_date = DateTimeMapper.localDateToString(session.sessionDate),
            first_attention_date = DateTimeMapper.localDateToString(session.firstAttentionDate),
            motivo_principal = session.motivoPrincipal,
            otros_motivos = session.otrosMotivos,
            updated_at = DateTimeMapper.instantToString(session.updatedAt),
            family_notes = session.familyNotes,
            id = session.id
        )
    }

    override suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.Default) {
        sessions.deleteSession(sessionId)
    }

    override suspend fun upsertProblemChains(entries: List<ProblemChainEntry>) = withContext(Dispatchers.Default) {
        if (entries.isEmpty()) return@withContext
        val sessionId = entries.first().sessionId
        chains.deleteProblemChainsBySession(sessionId)
        entries.forEach { entry ->
            chains.insertProblemChain(
                session_id = entry.sessionId,
                label = entry.label,
                vulnerabilidades = entry.vulnerabilidades,
                evento_desencadenante = entry.eventoDesencadenante,
                eslabones = entry.eslabones,
                problemas_conducta = entry.problemasConducta,
                consecuentes = entry.consecuentes
            )
        }
    }

    override suspend fun getProblemChains(sessionId: String): List<ProblemChainEntry> = withContext(Dispatchers.Default) {
        chains.selectProblemChainsBySession(sessionId).executeAsList().map { it.toDomain() }
    }

    override suspend fun upsertProblemGoals(goalsModel: ProblemGoals) = withContext(Dispatchers.Default) {
        goals.upsertProblemGoals(
            session_id = goalsModel.sessionId,
            metas = goalsModel.metas
        )
    }

    override suspend fun getProblemGoals(sessionId: String): ProblemGoals? = withContext(Dispatchers.Default) {
        goals.selectProblemGoals(sessionId).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun upsertPsychometrics(data: PsychometricData) = withContext(Dispatchers.Default) {
        psychometrics.upsertPsychometrics(
            session_id = data.sessionId,
            coeficiente_valor = data.coeficienteValor,
            coeficiente_clasificacion = data.coeficienteClasificacion,
            temperamento = data.temperamento,
            personalidad = data.personalidad,
            atencion = data.atencion,
            problemas_conducta = data.problemasConducta,
            dinamica_familiar = data.dinamicaFamiliar,
            otros_interes = data.otrosInteres
        )
    }

    override suspend fun getPsychometrics(sessionId: String): PsychometricData? = withContext(Dispatchers.Default) {
        psychometrics.selectPsychometrics(sessionId).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun upsertDysregulation(data: DysregulationAreas) = withContext(Dispatchers.Default) {
        dysregulation.upsertDysregulation(
            session_id = data.sessionId,
            emocional = data.emocional,
            conductual = data.conductual,
            interpersonal = data.interpersonal,
            self_valores = data.selfValores,
            cognitiva = data.cognitiva,
            resumen = data.resumen,
            bsl23_aplicado = if (data.bsl23Aplicado) 1 else 0
        )
    }

    override suspend fun getDysregulation(sessionId: String): DysregulationAreas? = withContext(Dispatchers.Default) {
        dysregulation.selectDysregulation(sessionId).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun upsertBiosocial(data: BiosocialModel) = withContext(Dispatchers.Default) {
        biosocial.upsertBiosocial(
            session_id = data.sessionId,
            vulnerabilidad_emocional = data.vulnerabilidadEmocional,
            sensibilidad = data.sensibilidad,
            intensidad = data.intensidad,
            lento_retorno_calma = data.lentoRetornoCalma,
            invalidacion_ambiental = data.invalidacionAmbiental,
            criticar_emociones = data.criticarEmociones,
            otros = data.otros
        )
    }

    override suspend fun getBiosocial(sessionId: String): BiosocialModel? = withContext(Dispatchers.Default) {
        biosocial.selectBiosocial(sessionId).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun upsertTreatmentObjectives(objectivesList: List<TreatmentObjective>) = withContext(Dispatchers.Default) {
        if (objectivesList.isEmpty()) return@withContext
        val sessionId = objectivesList.first().sessionId
        objectives.deleteTreatmentObjectivesBySession(sessionId)
        objectivesList.forEach { objective ->
            objectives.insertTreatmentObjective(
                session_id = objective.sessionId,
                stage = objective.stage.name,
                field_ = objective.field,
                value_ = objective.value
            )
        }
    }

    override suspend fun getTreatmentObjectives(sessionId: String): List<TreatmentObjective> = withContext(Dispatchers.Default) {
        objectives.selectTreatmentObjectives(sessionId).executeAsList().map { it.toDomain() }
    }

    override suspend fun upsertProblemAnalyses(analysesList: List<ProblemAnalysis>) = withContext(Dispatchers.Default) {
        if (analysesList.isEmpty()) return@withContext
        val sessionId = analysesList.first().sessionId
        analyses.deleteProblemAnalysisBySession(sessionId)
        analysesList.forEach { analysis ->
            analyses.insertProblemAnalysis(
                session_id = analysis.sessionId,
                problem_number = analysis.problemNumber.toLong(),
                vulnerabilidad = analysis.vulnerabilidad,
                evento_externo = analysis.eventoExterno,
                pensamientos = analysis.pensamientos,
                sensaciones = analysis.sensaciones,
                impulsos = analysis.impulsos,
                emociones = analysis.emociones,
                consecuencias_inmediatas = analysis.consecuenciasInmediatas,
                consecuencias_demoradas = analysis.consecuenciasDemoradas,
                plan_crisis = analysis.planCrisis,
                analisis_solucion = analysis.analisisSolucion,
                comportamiento = analysis.comportamiento
            )
        }
    }

    override suspend fun getProblemAnalyses(sessionId: String): List<ProblemAnalysis> = withContext(Dispatchers.Default) {
        analyses.selectProblemAnalysisBySession(sessionId).executeAsList().map { it.toDomain() }
    }

    override suspend fun upsertEvolutionNotes(notesList: List<EvolutionNote>) = withContext(Dispatchers.Default) {
        if (notesList.isEmpty()) return@withContext
        val sessionId = notesList.first().sessionId
        evolution.deleteEvolutionNotesBySession(sessionId)
        notesList.forEach { note ->
            evolution.insertEvolutionNote(
                session_id = note.sessionId,
                titulo = note.titulo,
                nota_fecha = note.notaFecha,
                comportamiento_trabajado = note.comportamientoTrabajado,
                apuntes = note.apuntes,
                tareas = note.tareas
            )
        }
    }

    override suspend fun getEvolutionNotes(sessionId: String): List<EvolutionNote> = withContext(Dispatchers.Default) {
        evolution.selectEvolutionNotesBySession(sessionId).executeAsList().map { it.toDomain() }
    }

    override suspend fun upsertTasks(tasksModel: SessionTasks) = withContext(Dispatchers.Default) {
        tasks.upsertSessionTasks(
            session_id = tasksModel.sessionId,
            descripcion = tasksModel.descripcion
        )
    }

    override suspend fun getTasks(sessionId: String): SessionTasks? = withContext(Dispatchers.Default) {
        tasks.selectSessionTasks(sessionId).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun addAttachment(attachmentModel: Attachment) = withContext(Dispatchers.Default) {
        attachments.insertAttachment(
            id = attachmentModel.id,
            session_id = attachmentModel.sessionId,
            display_name = attachmentModel.displayName,
            stored_name = attachmentModel.storedName,
            mime_type = attachmentModel.mimeType,
            size_bytes = attachmentModel.sizeBytes,
            sha256 = attachmentModel.sha256,
            created_at = DateTimeMapper.instantToString(attachmentModel.createdAt)
        )
    }

    override suspend fun getAttachments(sessionId: String): List<Attachment> = withContext(Dispatchers.Default) {
        attachments.selectAttachmentsBySession(sessionId).executeAsList().map { it.toDomain() }
    }

    override suspend fun removeAttachment(attachmentId: String) = withContext(Dispatchers.Default) {
        attachments.deleteAttachment(attachmentId)
    }

    override suspend fun appendHistory(entry: SessionHistoryEntry) = withContext(Dispatchers.Default) {
        history.insertHistoryEntry(
            session_id = entry.sessionId,
            change_type = entry.changeType,
            changed_at = DateTimeMapper.instantToString(entry.changedAt),
            payload = entry.payload
        )
    }

    override suspend fun getHistory(sessionId: String): List<SessionHistoryEntry> = withContext(Dispatchers.Default) {
        history.selectHistoryBySession(sessionId).executeAsList().map { it.toDomain() }
    }
}
