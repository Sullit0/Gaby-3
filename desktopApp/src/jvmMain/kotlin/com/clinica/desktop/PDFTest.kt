package com.clinica.desktop

import com.clinica.app.form.SessionFormState
import com.clinica.domain.model.*
import kotlinx.datetime.LocalDate
import java.nio.file.Paths
import java.util.UUID

fun main() {
    println("Probando generador de PDFs...")
    
    // Crear datos de prueba
    val testState = createTestSessionFormState()
    
    // Generar PDF
    val outputPath = Paths.get("test_output.pdf")
    val success = PDFGenerator.generatePDF(testState, outputPath)
    
    if (success) {
        println("PDF generado exitosamente en: ${outputPath.toAbsolutePath()}")
    } else {
        println("Error al generar el PDF")
    }
}

fun createTestSessionFormState(): SessionFormState {
    return SessionFormState(
        patient = Patient(
            id = UUID.randomUUID().toString(),
            displayName = "Juan Pérez García",
            firstName = "Juan",
            lastName = "Pérez García",
            dni = "12345678",
            gender = "Masculino",
            birthDate = LocalDate(1990, 5, 15),
            phone = "987654321",
            address = "Av. Principal 123, Lima",
            createdAt = kotlinx.datetime.Clock.System.now(),
            updatedAt = kotlinx.datetime.Clock.System.now()
        ),
        session = Session(
            id = UUID.randomUUID().toString(),
            patientId = UUID.randomUUID().toString(),
            sessionCode = 1,
            sessionDate = LocalDate(2024, 1, 10),
            motivoPrincipal = "Ansiedad social y dificultad para relacionarse",
            otrosMotivos = "Problemas de sueño y estrés laboral",
            firstAttentionDate = LocalDate(2024, 1, 10),
            familyNotes = "Notas familiares de prueba",
            createdAt = kotlinx.datetime.Clock.System.now(),
            updatedAt = kotlinx.datetime.Clock.System.now()
        ),
        familyNotes = "Padre separado, madre trabajadora, dos hermanos mayores. Relación familiar distante pero sin conflictos graves.",
        problemChains = listOf(
            ProblemChainEntry(
                id = 1L,
                sessionId = UUID.randomUUID().toString(),
                label = "Problema 1",
                vulnerabilidades = "Baja autoestima, miedo al rechazo",
                eventoDesencadenante = "Situaciones sociales nuevas",
                eslabones = "Evitación, pensamientos negativos",
                problemasConducta = "Aislamiento, ataques de pánico",
                consecuentes = "Deterioro social, soledad"
            )
        ),
        problemGoals = ProblemGoals(
            sessionId = UUID.randomUUID().toString(),
            metas = "Mejorar habilidades sociales, reducir ansiedad, aumentar autoestima"
        ),
        psychometrics = PsychometricData(
            sessionId = UUID.randomUUID().toString(),
            coeficienteValor = "110",
            coeficienteClasificacion = "Superior",
            temperamento = "Introversión moderada",
            personalidad = "Ansioso, perfeccionista",
            atencion = "Normal",
            problemasConducta = "Leves",
            dinamicaFamiliar = "Funcional",
            otrosInteres = "Alta motivación para el cambio"
        ),
        dysregulation = DysregulationAreas(
            sessionId = UUID.randomUUID().toString(),
            emocional = "Labilidad emocional moderada",
            conductual = "Impulsividad controlada",
            interpersonal = "Dificultad para establecer límites",
            selfValores = "Identidad en desarrollo",
            cognitiva = "Distorsiones cognitivas presentes",
            resumen = "Desregulación moderada en múltiples áreas",
            bsl23Aplicado = true
        ),
        biosocial = BiosocialModel(
            sessionId = UUID.randomUUID().toString(),
            vulnerabilidadEmocional = "Alta sensibilidad emocional",
            sensibilidad = "Reacciona intensamente a estímulos",
            intensidad = "Emociones muy intensas",
            lentoRetornoCalma = "Tarda en calmarse después de alterarse",
            invalidacionAmbiental = "Familia no valida sus emociones",
            criticarEmociones = "Le dicen que es demasiado sensible",
            otros = "Modelo biosocial claramente presente"
        ),
        treatmentObjectives = listOf(
            TreatmentObjective(
                id = 1L,
                sessionId = UUID.randomUUID().toString(),
                stage = TreatmentObjective.Stage.ETAPA_1,
                field = "field1",
                value = "Practicar mindfulness diario"
            )
        ),
        problemAnalyses = listOf(
            ProblemAnalysis(
                id = 1L,
                sessionId = UUID.randomUUID().toString(),
                problemNumber = 1,
                comportamiento = "Ansiedad social",
                analisisSolucion = "Desarrollar estrategias de afrontamiento",
                vulnerabilidad = "Sensibilidad al rechazo",
                eventoExterno = "Críticas de compañeros",
                pensamientos = "Nadie me aprecia",
                sensaciones = "Opresión en el pecho",
                impulsos = "Huir de la situación",
                emociones = "Ansiedad, tristeza",
                consecuenciasInmediatas = "Alivio temporal",
                consecuenciasDemoradas = "Aislamiento social",
                planCrisis = "Técnicas de respiración y contacto con terapeuta"
            )
        ),
        evolutionNotes = listOf(
            EvolutionNote(
                id = 1L,
                sessionId = UUID.randomUUID().toString(),
                titulo = "Sesión 1",
                notaFecha = LocalDate(2024, 1, 15).toString(),
                comportamientoTrabajado = "Ansiedad social",
                apuntes = "Paciente muestra buena disposición al tratamiento. Se trabajó en técnicas de respiración.",
                tareas = "Practicar respiración diafragmática 3 veces al día"
            )
        ),
        attachments = emptyList(),
        tasks = null,
        history = emptyList()
    )
}
