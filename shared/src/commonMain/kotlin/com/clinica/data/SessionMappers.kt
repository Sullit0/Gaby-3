package com.clinica.data

import com.clinica.domain.model.*
import kotlinx.datetime.Instant

internal fun Patient_sessions.toDomain(): Session = Session(
    id = id,
    patientId = patient_id,
    sessionCode = session_code.toInt(),
    sessionDate = DateTimeMapper.stringToLocalDate(session_date),
    firstAttentionDate = DateTimeMapper.stringToLocalDate(first_attention_date),
    motivoPrincipal = motivo_principal,
    otrosMotivos = otros_motivos,
    familyNotes = family_notes,
    createdAt = Instant.parse(created_at),
    updatedAt = Instant.parse(updated_at)
)

internal fun Session_problem_chains.toDomain(): ProblemChainEntry = ProblemChainEntry(
    id = id,
    sessionId = session_id,
    label = label,
    vulnerabilidades = vulnerabilidades,
    eventoDesencadenante = evento_desencadenante,
    eslabones = eslabones,
    problemasConducta = problemas_conducta,
    consecuentes = consecuentes
)

internal fun Session_problem_goals.toDomain(): ProblemGoals = ProblemGoals(
    sessionId = session_id,
    metas = metas
)

internal fun Session_psychometrics.toDomain(): PsychometricData = PsychometricData(
    sessionId = session_id,
    coeficienteValor = coeficiente_valor,
    coeficienteClasificacion = coeficiente_clasificacion,
    temperamento = temperamento,
    personalidad = personalidad,
    atencion = atencion,
    problemasConducta = problemas_conducta,
    dinamicaFamiliar = dinamica_familiar,
    otrosInteres = otros_interes
)

internal fun Session_dysregulation.toDomain(): DysregulationAreas = DysregulationAreas(
    sessionId = session_id,
    emocional = emocional,
    conductual = conductual,
    interpersonal = interpersonal,
    selfValores = self_valores,
    cognitiva = cognitiva,
    resumen = resumen,
    bsl23Aplicado = bsl23_aplicado != 0L
)

internal fun Session_biosocial.toDomain(): BiosocialModel = BiosocialModel(
    sessionId = session_id,
    vulnerabilidadEmocional = vulnerabilidad_emocional,
    sensibilidad = sensibilidad,
    intensidad = intensidad,
    lentoRetornoCalma = lento_retorno_calma,
    invalidacionAmbiental = invalidacion_ambiental,
    criticarEmociones = criticar_emociones,
    otros = otros
)

internal fun Session_tasks.toDomain(): SessionTasks = SessionTasks(
    sessionId = session_id,
    descripcion = descripcion
)

internal fun Session_problem_analysis.toDomain(): ProblemAnalysis = ProblemAnalysis(
    id = id,
    sessionId = session_id,
    problemNumber = problem_number.toInt(),
    comportamiento = comportamiento,
    vulnerabilidad = vulnerabilidad,
    eventoExterno = evento_externo,
    pensamientos = pensamientos,
    sensaciones = sensaciones,
    impulsos = impulsos,
    emociones = emociones,
    consecuenciasInmediatas = consecuencias_inmediatas,
    consecuenciasDemoradas = consecuencias_demoradas,
    planCrisis = plan_crisis,
    analisisSolucion = analisis_solucion
)

internal fun Session_treatment_objectives.toDomain(): TreatmentObjective = TreatmentObjective(
    id = id,
    sessionId = session_id,
    stage = TreatmentObjective.Stage.valueOf(stage),
    field = field_,
    value = value_
)

internal fun Session_evolution_notes.toDomain(): EvolutionNote = EvolutionNote(
    id = id,
    sessionId = session_id,
    titulo = titulo,
    notaFecha = nota_fecha,
    comportamientoTrabajado = comportamiento_trabajado,
    apuntes = apuntes,
    tareas = tareas
)

internal fun Attachments.toDomain(): Attachment = Attachment(
    id = id,
    sessionId = session_id,
    displayName = display_name,
    storedName = stored_name,
    mimeType = mime_type,
    sizeBytes = size_bytes?.toLong(),
    sha256 = sha256,
    createdAt = Instant.parse(created_at)
)

internal fun Session_history.toDomain(): SessionHistoryEntry = SessionHistoryEntry(
    id = id,
    sessionId = session_id,
    changeType = change_type,
    changedAt = Instant.parse(changed_at),
    payload = payload
)
