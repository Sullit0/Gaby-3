package com.clinica.app.form

import com.clinica.domain.model.Patient
import com.clinica.domain.model.Session
import com.clinica.domain.model.SessionTasks
import com.clinica.domain.model.Attachment
import com.clinica.domain.repository.PatientRepository
import com.clinica.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import com.benasher44.uuid.uuid4

class SessionFormViewModel(
    private val patientRepository: PatientRepository,
    private val sessionRepository: SessionRepository,
    private val scope: CoroutineScope
) {

    private val _state = MutableStateFlow(SessionFormState())
    val state: StateFlow<SessionFormState> = _state.asStateFlow()

    private var currentSessionId: String? = null
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
                _state.value = _state.value.copy(isLoading = false, error = err.message)
            }
        }
    }

    private suspend fun ensureSession(patientId: String) {
        val sessions = sessionRepository.observeSessions(patientId).firstOrNull()
        val session = sessions?.firstOrNull()
            ?: sessionRepository.createSession(patientId)
        currentSessionId = session.id
        val tasks = sessionRepository.getTasks(session.id)
        val attachments = sessionRepository.getAttachments(session.id)
        _state.value = SessionFormState(
            patient = patientRepository.getById(patientId),
            session = session,
            tasks = tasks,
            attachments = attachments,
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
        scope.launch(Dispatchers.Default) {
            runCatching {
                patientRepository.upsert(patient)
                _state.value = _state.value.copy(patient = patient)
            }.onFailure { err ->
                _state.value = _state.value.copy(error = err.message)
            }
        }
    }

    fun updateSession(block: (Session) -> Session) {
        val session = _state.value.session ?: return
        val now = Clock.System.now()
        val updated = block(session.copy(updatedAt = now)).copy(updatedAt = now)
        scope.launch(Dispatchers.Default) {
            runCatching {
                sessionRepository.updateSession(updated)
                _state.value = _state.value.copy(session = updated)
            }.onFailure { err ->
                _state.value = _state.value.copy(error = err.message)
            }
        }
    }

    fun updateTasks(description: String?) {
        val sessionId = currentSessionId ?: return
        val model = SessionTasks(sessionId, description)
        scope.launch(Dispatchers.Default) {
            runCatching {
                sessionRepository.upsertTasks(model)
                _state.value = _state.value.copy(tasks = model)
            }.onFailure { err ->
                _state.value = _state.value.copy(error = err.message)
            }
        }
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

    fun registerAttachment(attachment: Attachment) {
        scope.launch(Dispatchers.Default) {
            runCatching {
                sessionRepository.addAttachment(attachment)
                val list = sessionRepository.getAttachments(attachment.sessionId)
                _state.value = _state.value.copy(attachments = list)
            }.onFailure { err ->
                _state.value = _state.value.copy(error = err.message)
            }
        }
    }

    fun removeAttachment(id: String) {
        val sessionId = currentSessionId ?: return
        scope.launch(Dispatchers.Default) {
            runCatching {
                sessionRepository.removeAttachment(id)
                val list = sessionRepository.getAttachments(sessionId)
                _state.value = _state.value.copy(attachments = list)
            }.onFailure { err ->
                _state.value = _state.value.copy(error = err.message)
            }
        }
    }
}

private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.valueOrNull(): T? {
    var value: T? = null
    collect { collected ->
        value = collected
    }
    return value
}
