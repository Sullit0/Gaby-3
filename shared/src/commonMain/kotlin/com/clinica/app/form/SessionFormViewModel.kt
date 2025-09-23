package com.clinica.app.form

import com.benasher44.uuid.uuid4
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
import com.clinica.domain.repository.PatientRepository
import com.clinica.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate

class SessionFormViewModel(
    val patientRepository: PatientRepository,
    val sessionRepository: SessionRepository,
    private val scope: CoroutineScope
) {

    private val _state = MutableStateFlow(SessionFormState())
    val state: StateFlow<SessionFormState> = _state.asStateFlow()

    private var currentSessionId: String? = null
    private var tempIdSeed = -1L

    fun loadDefaultPatient(patientName: String = "Paciente Demo") {
        scope.launch(Dispatchers.Default) {
            runCatching {
                var patient = patientRepository.observeAll()
                    .firstOrNull()
                    ?.firstOrNull { it.displayName == patientName }
                if (patient == null) {
                    val now = Clock.System.now()
                    patient = Patient(
                        id = uuid4().toString(),
                        displayName = patientName,
                        firstName = null,
                        lastName = null,
                        dni = null,
                        gender = null,
                        birthDate = null,
                        phone = null,
                        address = null,
                        createdAt = now,
                        updatedAt = now
                    )
                    patientRepository.upsert(patient)
                }
                ensureSession(patient.id)
            }.onFailure { err ->
                handleError(err)
            }
        }
    }

    private suspend fun ensureSession(patientId: String) {
        val sessions = sessionRepository.observeSessions(patientId).firstOrNull()
        val session = sessions?.firstOrNull() ?: sessionRepository.createSession(patientId)
        currentSessionId = session.id

        val sessionId = session.id
        val tasks = sessionRepository.getTasks(sessionId)
        val attachments = sessionRepository.getAttachments(sessionId)
        val problemChains = ensureProblemChainDefaults(sessionId, sessionRepository.getProblemChains(sessionId))
        val problemGoals = sessionRepository.getProblemGoals(sessionId)
        val psychometrics = sessionRepository.getPsychometrics(sessionId)
        val dysregulation = sessionRepository.getDysregulation(sessionId)
        val biosocial = sessionRepository.getBiosocial(sessionId)
        val treatmentObjectives = ensureTreatmentObjectiveDefaults(sessionId, sessionRepository.getTreatmentObjectives(sessionId))
        val problemAnalyses = ensureProblemAnalysisDefaults(sessionId, sessionRepository.getProblemAnalyses(sessionId))
        val evolutionNotes = ensureEvolutionNoteDefaults(sessionId, sessionRepository.getEvolutionNotes(sessionId))
        val history = sessionRepository.getHistory(sessionId)

        _state.value = SessionFormState(
            patient = patientRepository.getById(patientId),
            session = session,
            tasks = tasks,
            attachments = attachments,
            familyNotes = session.familyNotes.orEmpty(),
            problemChains = problemChains,
            problemGoals = problemGoals,
            psychometrics = psychometrics,
            dysregulation = dysregulation,
            biosocial = biosocial,
            treatmentObjectives = treatmentObjectives,
            problemAnalyses = problemAnalyses,
            evolutionNotes = evolutionNotes,
            history = history,
            isLoading = false,
            error = null
        )
    }

    fun updatePatientName(name: String) {
        val patient = _state.value.patient ?: return
        updatePatient(patient.copy(displayName = name, updatedAt = Clock.System.now()))
    }

    fun updatePatientField(block: (Patient) -> Patient) {
        val patient = _state.value.patient ?: return
        val now = Clock.System.now()
        val updated = block(patient.copy(updatedAt = now)).copy(updatedAt = now)
        updatePatient(updated)
    }

    private fun updatePatient(patient: Patient) {
        _state.update { it.copy(patient = patient) }
        scope.launch(Dispatchers.Default) {
            runCatching {
                patientRepository.upsert(patient)
            }.onFailure { err ->
                handleError(err)
            }
        }
    }

    fun updateSession(block: (Session) -> Session) {
        val session = _state.value.session ?: return
        val now = Clock.System.now()
        val updated = block(session.copy(updatedAt = now)).copy(updatedAt = now)
        _state.update {
            it.copy(
                session = updated,
                familyNotes = updated.familyNotes.orEmpty()
            )
        }
        scope.launch(Dispatchers.Default) {
            runCatching {
                sessionRepository.updateSession(updated)
            }.onFailure { err ->
                handleError(err)
            }
        }
    }

    fun loadPatientById(patientId: String) {
        scope.launch(Dispatchers.Default) {
            runCatching {
                val patient = patientRepository.getById(patientId)
                if (patient != null) {
                    currentSessionId = patient.id
                    val sessions = sessionRepository.observeSessions(patientId).firstOrNull()
                    val session = sessions?.firstOrNull() ?: sessionRepository.createSession(patientId)
                    val tasks = sessionRepository.getTasks(session.id)
                    val attachments = sessionRepository.getAttachments(session.id)
                    val problemChains = ensureProblemChainDefaults(session.id, sessionRepository.getProblemChains(session.id))
                    val problemGoals = sessionRepository.getProblemGoals(session.id)
                    val psychometrics = sessionRepository.getPsychometrics(session.id)
                    val dysregulation = sessionRepository.getDysregulation(session.id)
                    val biosocial = sessionRepository.getBiosocial(session.id)
                    val treatmentObjectives = ensureTreatmentObjectiveDefaults(session.id, sessionRepository.getTreatmentObjectives(session.id))
                    val problemAnalyses = ensureProblemAnalysisDefaults(session.id, sessionRepository.getProblemAnalyses(session.id))
                    val evolutionNotes = ensureEvolutionNoteDefaults(session.id, sessionRepository.getEvolutionNotes(session.id))
                    val history = sessionRepository.getHistory(session.id)

                    _state.value = SessionFormState(
                        patient = patient,
                        session = session,
                        tasks = tasks,
                        attachments = attachments,
                        familyNotes = session.familyNotes.orEmpty(),
                        problemChains = problemChains,
                        problemGoals = problemGoals,
                        psychometrics = psychometrics,
                        dysregulation = dysregulation,
                        biosocial = biosocial,
                        treatmentObjectives = treatmentObjectives,
                        problemAnalyses = problemAnalyses,
                        evolutionNotes = evolutionNotes,
                        history = history,
                        isLoading = false,
                        error = null
                    )
                } else {
                    _state.update { it.copy(error = "Paciente no encontrado") }
                }
            }.onFailure { err ->
                handleError(err)
            }
        }
    }

    fun updateFamilyNotes(notes: String) {
        updateSession { it.copy(familyNotes = notes) }
        _state.update { it.copy(familyNotes = notes) }
    }

    fun updateTasks(description: String?) {
        val sessionId = currentSessionId ?: return
        val model = SessionTasks(sessionId, description?.takeIf { it.isNotBlank() })
        _state.update { it.copy(tasks = model) }
        scope.launch(Dispatchers.Default) {
            runCatching {
                sessionRepository.upsertTasks(model)
            }.onFailure { err ->
                handleError(err)
            }
        }
    }

    fun updateProblemChain(label: String, update: (ProblemChainEntry) -> ProblemChainEntry) {
        val sessionId = currentSessionId ?: return
        val current = if (_state.value.problemChains.isEmpty()) {
            ensureProblemChainDefaults(sessionId, emptyList())
        } else {
            _state.value.problemChains
        }
        val mutable = current.toMutableList()
        val index = mutable.indexOfFirst { it.label == label }
        val base = if (index >= 0) mutable[index] else defaultProblemChain(label, sessionId)
        val updated = update(base).copy(sessionId = sessionId)
        if (index >= 0) {
            mutable[index] = updated
        } else {
            mutable.add(updated)
        }
        _state.update { it.copy(problemChains = mutable) }
        scope.launch(Dispatchers.Default) {
            runCatching {
                sessionRepository.upsertProblemChains(mutable.map { it.copy(sessionId = sessionId) })
            }.onFailure { err -> handleError(err) }
        }
    }

    fun updateProblemGoals(value: String?) {
        val sessionId = currentSessionId ?: return
        val model = ProblemGoals(sessionId, value?.takeIf { it.isNotBlank() })
        _state.update { it.copy(problemGoals = model) }
        scope.launch(Dispatchers.Default) {
            runCatching {
                sessionRepository.upsertProblemGoals(model)
            }.onFailure { err -> handleError(err) }
        }
    }

    fun updatePsychometrics(transform: (PsychometricData) -> PsychometricData) {
        val sessionId = currentSessionId ?: return
        val base = _state.value.psychometrics ?: defaultPsychometrics(sessionId)
        val updated = transform(base).copy(sessionId = sessionId)
        _state.update { it.copy(psychometrics = updated) }
        scope.launch(Dispatchers.Default) {
            runCatching {
                sessionRepository.upsertPsychometrics(updated)
            }.onFailure { err -> handleError(err) }
        }
    }

    fun updateDysregulation(transform: (DysregulationAreas) -> DysregulationAreas) {
        val sessionId = currentSessionId ?: return
        val base = _state.value.dysregulation ?: defaultDysregulation(sessionId)
        val updated = transform(base).copy(sessionId = sessionId)
        _state.update { it.copy(dysregulation = updated) }
        scope.launch(Dispatchers.Default) {
            runCatching {
                sessionRepository.upsertDysregulation(updated)
            }.onFailure { err -> handleError(err) }
        }
    }

    fun updateBiosocial(transform: (BiosocialModel) -> BiosocialModel) {
        val sessionId = currentSessionId ?: return
        val base = _state.value.biosocial ?: defaultBiosocial(sessionId)
        val updated = transform(base).copy(sessionId = sessionId)
        _state.update { it.copy(biosocial = updated) }
        scope.launch(Dispatchers.Default) {
            runCatching {
                sessionRepository.upsertBiosocial(updated)
            }.onFailure { err -> handleError(err) }
        }
    }

    fun updateTreatmentObjective(stage: TreatmentObjective.Stage, field: String, value: String?) {
        val sessionId = currentSessionId ?: return
        val current = ensureTreatmentObjectivesInState(sessionId)
        val mutable = current.toMutableList()
        val index = mutable.indexOfFirst { it.stage == stage && it.field == field }
        val base = if (index >= 0) mutable[index] else TreatmentObjective(nextTempId(), sessionId, stage, field, null)
        val updated = base.copy(value = value, sessionId = sessionId)
        if (index >= 0) {
            mutable[index] = updated
        } else {
            mutable.add(updated)
        }
        persistTreatmentObjectives(sessionId, mutable)
    }

    fun updateProblemAnalysis(problemNumber: Int, transform: (ProblemAnalysis) -> ProblemAnalysis) {
        val sessionId = currentSessionId ?: return
        val current = ensureProblemAnalysesInState(sessionId)
        val mutable = current.toMutableList()
        val index = mutable.indexOfFirst { it.problemNumber == problemNumber }
        val base = if (index >= 0) mutable[index] else defaultProblemAnalysis(sessionId, problemNumber)
        val updated = transform(base).copy(sessionId = sessionId, problemNumber = problemNumber)
        if (index >= 0) {
            mutable[index] = updated
        } else {
            mutable.add(updated)
        }
        persistProblemAnalyses(sessionId, mutable)
    }

    fun addProblemAnalysis() {
        val sessionId = currentSessionId ?: return
        val current = ensureProblemAnalysesInState(sessionId)
        val nextNumber = (current.maxOfOrNull { it.problemNumber } ?: 0) + 1
        val updated = current + defaultProblemAnalysis(sessionId, nextNumber)
        persistProblemAnalyses(sessionId, updated)
    }

    fun removeProblemAnalysis(problemNumber: Int) {
        val sessionId = currentSessionId ?: return
        val updated = _state.value.problemAnalyses.filterNot { it.problemNumber == problemNumber }
        persistProblemAnalyses(sessionId, updated)
    }

    fun updateEvolutionNote(id: Long, transform: (EvolutionNote) -> EvolutionNote) {
        val sessionId = currentSessionId ?: return
        val current = ensureEvolutionNotesInState(sessionId)
        val mutable = current.toMutableList()
        val index = mutable.indexOfFirst { it.id == id }
        val base = if (index >= 0) mutable[index] else defaultEvolutionNote(sessionId, mutable.size)
        val updated = transform(base).copy(sessionId = sessionId)
        if (index >= 0) {
            mutable[index] = updated
        } else {
            mutable.add(updated)
        }
        persistEvolutionNotes(sessionId, mutable)
    }

    fun addEvolutionNote() {
        val sessionId = currentSessionId ?: return
        val current = ensureEvolutionNotesInState(sessionId)
        val updated = current + defaultEvolutionNote(sessionId, current.size)
        persistEvolutionNotes(sessionId, updated)
    }

    fun removeEvolutionNote(id: Long) {
        val sessionId = currentSessionId ?: return
        val updated = _state.value.evolutionNotes.filterNot { it.id == id }
        persistEvolutionNotes(sessionId, updated)
    }

    fun updateBirthDate(date: LocalDate?) {
        updatePatientField { it.copy(birthDate = date) }
    }

    fun updateBirthDateRaw(input: String) {
        val parsed = input.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        updateBirthDate(parsed)
    }

    fun updateFirstAttentionDate(date: LocalDate?) {
        updateSession { it.copy(firstAttentionDate = date) }
    }

    fun updateFirstAttentionRaw(input: String) {
        val parsed = input.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        updateFirstAttentionDate(parsed)
    }

    fun updateSessionDate(date: LocalDate?) {
        updateSession { it.copy(sessionDate = date) }
    }

    fun updateMotivoPrincipal(value: String?) {
        updateSession { it.copy(motivoPrincipal = value) }
    }

    fun updateOtrosMotivos(value: String?) {
        updateSession { it.copy(otrosMotivos = value) }
    }

    fun updateDni(value: String?) {
        updatePatientField { it.copy(dni = value) }
    }

    fun updateDireccion(value: String?) {
        updatePatientField { it.copy(address = value) }
    }

    fun updatePhone(value: String?) {
        updatePatientField { it.copy(phone = value) }
    }

    fun updateGender(value: String?) {
        updatePatientField { it.copy(gender = value) }
    }

    fun dispose() {
        scope.coroutineContext[Job]?.cancel()
    }

    fun saveSession() {
        scope.launch(Dispatchers.Default) {
            runCatching {
                val currentState = _state.value
                val patient = currentState.patient
                val session = currentState.session

                if (patient != null && session != null) {
                    // Guardar paciente
                    patientRepository.upsert(patient)

                    // Guardar sesión principal
                    sessionRepository.updateSession(session)

                    // Guardar tareas
                    currentState.tasks?.let { tasks ->
                        sessionRepository.upsertTasks(tasks.copy(sessionId = session.id))
                    }

                    // Guardar cadenas de problemas
                    sessionRepository.upsertProblemChains(currentState.problemChains.map { it.copy(sessionId = session.id) })

                    // Guardar metas de problemas
                    currentState.problemGoals?.let { goals ->
                        sessionRepository.upsertProblemGoals(goals.copy(sessionId = session.id))
                    }

                    // Guardar datos psicométricos
                    currentState.psychometrics?.let { psychometrics ->
                        sessionRepository.upsertPsychometrics(psychometrics.copy(sessionId = session.id))
                    }

                    // Guardar áreas de desregulación
                    currentState.dysregulation?.let { dysregulation ->
                        sessionRepository.upsertDysregulation(dysregulation.copy(sessionId = session.id))
                    }

                    // Guardar modelo biosocial
                    currentState.biosocial?.let { biosocial ->
                        sessionRepository.upsertBiosocial(biosocial.copy(sessionId = session.id))
                    }

                    // Guardar objetivos de tratamiento
                    sessionRepository.upsertTreatmentObjectives(currentState.treatmentObjectives.map { it.copy(sessionId = session.id) })

                    // Guardar análisis de problemas
                    sessionRepository.upsertProblemAnalyses(currentState.problemAnalyses.map { it.copy(sessionId = session.id) })

                    // Guardar notas de evolución
                    sessionRepository.upsertEvolutionNotes(currentState.evolutionNotes.map { it.copy(sessionId = session.id) })

                    // Actualizar estado para indicar que se guardó correctamente
                    _state.update { it.copy(error = null) }
                }
            }.onFailure { err ->
                handleError(err)
            }
        }
    }

    fun registerAttachment(attachment: Attachment) {
        scope.launch(Dispatchers.Default) {
            runCatching {
                sessionRepository.addAttachment(attachment)
                val list = sessionRepository.getAttachments(attachment.sessionId)
                _state.update { it.copy(attachments = list) }
            }.onFailure { err ->
                handleError(err)
            }
        }
    }

    fun removeAttachment(id: String) {
        val sessionId = currentSessionId ?: return
        scope.launch(Dispatchers.Default) {
            runCatching {
                val attachment = _state.value.attachments.firstOrNull { it.id == id }
                sessionRepository.removeAttachment(id)
                attachment?.let { removeAttachmentTokenInternal(it) }
                val list = sessionRepository.getAttachments(sessionId)
                _state.update { it.copy(attachments = list) }
            }.onFailure { err ->
                handleError(err)
            }
        }
    }

    fun appendAttachmentToken(attachment: Attachment) {
        appendAttachmentTokenInternal(attachment)
    }

    fun removeAttachmentToken(attachment: Attachment) {
        removeAttachmentTokenInternal(attachment)
    }

    private fun appendAttachmentTokenInternal(attachment: Attachment) {
        val current = _state.value.tasks?.descripcion.orEmpty()
        val updated = appendToken(current, attachmentToken(attachment))
        updateTasks(updated)
    }

    private fun removeAttachmentTokenInternal(attachment: Attachment) {
        val current = _state.value.tasks?.descripcion.orEmpty()
        if (current.isEmpty()) return
        val updated = removeToken(current, attachmentToken(attachment))
        updateTasks(updated)
    }

    private fun attachmentToken(attachment: Attachment): String = "[${attachment.displayName}]"

    private fun appendToken(current: String, token: String): String {
        val tokenPattern = Regex("(?<=^|\\s)${Regex.escape(token)}(?=\\s|$)")
        if (tokenPattern.containsMatchIn(current)) return current
        if (current.isBlank()) return token
        val needsSpace = !current.last().isWhitespace()
        return buildString {
            append(current)
            if (needsSpace) append(' ')
            append(token)
        }
    }

    private fun removeToken(current: String, token: String): String {
        val index = current.indexOf(token)
        if (index < 0) return current
        var start = index
        var end = index + token.length
        if (start > 0 && current[start - 1].isWhitespace()) {
            start--
        }
        if (end < current.length && current[end].isWhitespace()) {
            end++
        }
        val trimmed = current.removeRange(start, end)
        return trimmed.replace("  ", " ").trim()
    }

    private fun ensureProblemChainDefaults(sessionId: String, stored: List<ProblemChainEntry>): List<ProblemChainEntry> {
        val labels = SessionFormMetadata.problemChainLabels
        val labelSet = labels.toSet()
        val indexed = stored.associateBy { it.label }
        val defaults = labels.map { label ->
            indexed[label] ?: defaultProblemChain(label, sessionId)
        }
        val extras = stored.filterNot { it.label in labelSet }
        return defaults + extras
    }

    private fun defaultProblemChain(label: String, sessionId: String): ProblemChainEntry =
        ProblemChainEntry(
            id = nextTempId(),
            sessionId = sessionId,
            label = label,
            vulnerabilidades = null,
            eventoDesencadenante = null,
            eslabones = null,
            problemasConducta = null,
            consecuentes = null
        )

    private fun ensureTreatmentObjectiveDefaults(sessionId: String, stored: List<TreatmentObjective>): List<TreatmentObjective> {
        val definitions = SessionFormMetadata.treatmentFields
        val byKey = stored.associateBy { it.stage to it.field }
        val defaults = definitions.map { definition ->
            byKey[definition.stage to definition.field]
                ?: TreatmentObjective(
                    id = nextTempId(),
                    sessionId = sessionId,
                    stage = definition.stage,
                    field = definition.field,
                    value = null
                )
        }
        val extras = stored.filterNot { storedObjective ->
            definitions.any { it.stage == storedObjective.stage && it.field == storedObjective.field }
        }
        return defaults + extras
    }

    private fun ensureTreatmentObjectivesInState(sessionId: String): List<TreatmentObjective> {
        val current = _state.value.treatmentObjectives
        return ensureTreatmentObjectiveDefaults(sessionId, current)
    }

    private fun persistTreatmentObjectives(sessionId: String, list: List<TreatmentObjective>) {
        _state.update { it.copy(treatmentObjectives = list) }
        scope.launch(Dispatchers.Default) {
            runCatching {
                sessionRepository.upsertTreatmentObjectives(list.map { it.copy(sessionId = sessionId) })
            }.onFailure { err -> handleError(err) }
        }
    }

    private fun ensureProblemAnalysisDefaults(sessionId: String, stored: List<ProblemAnalysis>): List<ProblemAnalysis> {
        val byNumber = stored.associateBy { it.problemNumber }
        val required = SessionFormMetadata.problemAnalysisNumbers.map { number ->
            byNumber[number] ?: defaultProblemAnalysis(sessionId, number)
        }
        val extras = stored.filterNot { it.problemNumber in SessionFormMetadata.problemAnalysisNumbers.toSet() }
        return (required + extras).sortedBy { it.problemNumber }
    }

    private fun ensureProblemAnalysesInState(sessionId: String): List<ProblemAnalysis> {
        val current = _state.value.problemAnalyses
        return ensureProblemAnalysisDefaults(sessionId, current)
    }

    private fun defaultProblemAnalysis(sessionId: String, number: Int): ProblemAnalysis =
        ProblemAnalysis(
            id = nextTempId(),
            sessionId = sessionId,
            problemNumber = number,
            comportamiento = null,
            vulnerabilidad = null,
            eventoExterno = null,
            pensamientos = null,
            sensaciones = null,
            impulsos = null,
            emociones = null,
            consecuenciasInmediatas = null,
            consecuenciasDemoradas = null,
            planCrisis = null,
            analisisSolucion = null
        )

    private fun persistProblemAnalyses(sessionId: String, list: List<ProblemAnalysis>) {
        val ordered = list.sortedBy { it.problemNumber }
        _state.update { it.copy(problemAnalyses = ordered) }
        scope.launch(Dispatchers.Default) {
            runCatching {
                sessionRepository.upsertProblemAnalyses(ordered.map { it.copy(sessionId = sessionId) })
            }.onFailure { err -> handleError(err) }
        }
    }

    private fun ensureEvolutionNoteDefaults(sessionId: String, stored: List<EvolutionNote>): List<EvolutionNote> {
        if (stored.isNotEmpty()) return stored
        return listOf(defaultEvolutionNote(sessionId, 0))
    }

    private fun ensureEvolutionNotesInState(sessionId: String): List<EvolutionNote> {
        val current = _state.value.evolutionNotes
        return if (current.isEmpty()) ensureEvolutionNoteDefaults(sessionId, emptyList()) else current
    }

    private fun defaultEvolutionNote(sessionId: String, index: Int): EvolutionNote =
        EvolutionNote(
            id = nextTempId(),
            sessionId = sessionId,
            titulo = "Sesión ${index + 1}",
            notaFecha = null,
            comportamientoTrabajado = null,
            apuntes = null,
            tareas = null
        )

    private fun persistEvolutionNotes(sessionId: String, list: List<EvolutionNote>) {
        _state.update { it.copy(evolutionNotes = list) }
        scope.launch(Dispatchers.Default) {
            runCatching {
                sessionRepository.upsertEvolutionNotes(list.map { it.copy(sessionId = sessionId) })
            }.onFailure { err -> handleError(err) }
        }
    }

    private fun defaultPsychometrics(sessionId: String): PsychometricData =
        PsychometricData(
            sessionId = sessionId,
            coeficienteValor = null,
            coeficienteClasificacion = null,
            temperamento = null,
            personalidad = null,
            atencion = null,
            problemasConducta = null,
            dinamicaFamiliar = null,
            otrosInteres = null
        )

    private fun defaultDysregulation(sessionId: String): DysregulationAreas =
        DysregulationAreas(
            sessionId = sessionId,
            emocional = null,
            conductual = null,
            interpersonal = null,
            selfValores = null,
            cognitiva = null,
            resumen = null,
            bsl23Aplicado = false
        )

    private fun defaultBiosocial(sessionId: String): BiosocialModel =
        BiosocialModel(
            sessionId = sessionId,
            vulnerabilidadEmocional = null,
            sensibilidad = null,
            intensidad = null,
            lentoRetornoCalma = null,
            invalidacionAmbiental = null,
            criticarEmociones = null,
            otros = null
        )

    private fun nextTempId(): Long = tempIdSeed--

    private fun handleError(err: Throwable) {
        _state.update { it.copy(error = err.message, isLoading = false) }
    }

    suspend fun createSessionStateForPatient(patientId: String): SessionFormState? {
        return try {
            val patient = patientRepository.getById(patientId) ?: return null
            val sessions = sessionRepository.observeSessions(patientId).firstOrNull()
            val session = sessions?.firstOrNull() ?: sessionRepository.createSession(patientId)
            val tasks = sessionRepository.getTasks(session.id)
            val attachments = sessionRepository.getAttachments(session.id)
            val problemChains = ensureProblemChainDefaults(session.id, sessionRepository.getProblemChains(session.id))
            val problemGoals = sessionRepository.getProblemGoals(session.id)
            val psychometrics = sessionRepository.getPsychometrics(session.id)
            val dysregulation = sessionRepository.getDysregulation(session.id)
            val biosocial = sessionRepository.getBiosocial(session.id)
            val treatmentObjectives = ensureTreatmentObjectiveDefaults(session.id, sessionRepository.getTreatmentObjectives(session.id))
            val problemAnalyses = ensureProblemAnalysisDefaults(session.id, sessionRepository.getProblemAnalyses(session.id))
            val evolutionNotes = ensureEvolutionNoteDefaults(session.id, sessionRepository.getEvolutionNotes(session.id))
            val history = sessionRepository.getHistory(session.id)

            SessionFormState(
                patient = patient,
                session = session,
                tasks = tasks,
                attachments = attachments,
                familyNotes = session.familyNotes.orEmpty(),
                problemChains = problemChains,
                problemGoals = problemGoals,
                psychometrics = psychometrics,
                dysregulation = dysregulation,
                biosocial = biosocial,
                treatmentObjectives = treatmentObjectives,
                problemAnalyses = problemAnalyses,
                evolutionNotes = evolutionNotes,
                history = history,
                isLoading = false,
                error = null
            )
        } catch (e: Exception) {
            null
        }
    }
}
