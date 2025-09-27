package com.clinica.desktop

import com.clinica.app.form.SessionFormState
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.periodUntil
import org.apache.poi.xwpf.usermodel.*
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.TableWidthType
import org.apache.poi.xwpf.usermodel.BreakType
import org.apache.poi.xwpf.usermodel.UnderlinePatterns
import org.apache.poi.xwpf.usermodel.VerticalAlign
import org.apache.poi.xwpf.usermodel.TableRowAlign
import org.apache.poi.xwpf.usermodel.Borders
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.awt.Desktop
import java.io.IOException

class WordDocumentGenerator {

    companion object {
        // Constantes para el diseño
        private const val FONT_SIZE_NORMAL = 10
        private const val FONT_SIZE_SMALL = 9
        private const val FONT_SIZE_HEADER = 12
        
        /**
         * Función principal para rellenar plantilla Word con datos del formulario
         * @param state Estado del formulario con todos los datos
         * @param templatePath Ruta de la plantilla .docx
         * @param outputPath Ruta donde se guardará el documento rellenado
         * @return true si se generó correctamente, false si hubo error
         */
        fun fillTemplateWithFormData(state: SessionFormState, templatePath: Path, outputPath: Path): Boolean {
            return try {
                // Verificar que existe la plantilla
                if (!templatePath.toFile().exists()) {
                    println("ERROR: No se encuentra la plantilla en: $templatePath")
                    return false
                }
                
                // Limpiar buffers antes de comenzar
                System.gc()
                
                // Cargar plantilla existente
                FileInputStream(templatePath.toFile()).use { input ->
                    XWPFDocument(input).use { document ->
                        // Rellenar la plantilla con los datos
                        replacePlaceholders(document, state)
                        
                        // Forzar limpieza de buffers durante el proceso
                        System.gc()
                        
                        // Guardar el documento rellenado
                        FileOutputStream(outputPath.toFile()).use { out ->
                            document.write(out)
                            // Forzar flush del stream
                            out.flush()
                        }
                    }
                }
                
                // Limpiar buffers después de completar
                System.gc()
                
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        /**
         * Función de conveniencia que rellena la plantilla y abre el Word automáticamente
         * @param state Estado del formulario con todos los datos
         * @param templatePath Ruta de la plantilla .docx
         * @param fileName Nombre del archivo a generar
         * @return Ruta del archivo generado o null si hubo error
         */
        fun fillTemplateForPatient(state: SessionFormState, templatePath: Path, fileName: String = "ficha_paciente.docx"): Path? {
            val downloadsDir = Paths.get(System.getProperty("user.home"), "Downloads")
            val outputPath = downloadsDir.resolve(fileName)
            
            return if (fillTemplateWithFormData(state, templatePath, outputPath)) {
                // Abrir el documento automáticamente
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(outputPath.toFile())
                }
                outputPath
            } else {
                null
            }
        }

        /**
         * Función que rellena la plantilla y envía automáticamente a la impresora
         * @param state Estado del formulario con todos los datos
         * @param templatePath Ruta de la plantilla .docx
         * @return Ruta del archivo generado o null si hubo error
         */
        fun fillTemplateAndPrint(state: SessionFormState, templatePath: Path): Path? {
            val tempDir = Paths.get(System.getProperty("java.io.tmpdir"))
            val patientName = state.patient?.displayName?.replace(" ", "_")?.lowercase() ?: "paciente"
            val timestamp = Clock.System.now().toString()
                .replace(":", "-").replace(".", "-")
            val fileName = "ficha_${patientName}_${timestamp}.docx"
            val outputPath = tempDir.resolve(fileName)
            
            return if (fillTemplateWithFormData(state, templatePath, outputPath)) {
                // Enviar a impresión automáticamente
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().print(outputPath.toFile())
                        println("Documento enviado a impresión: $outputPath")
                    } catch (e: Exception) {
                        println("Error al enviar a impresión: ${e.message}")
                        // Si falla la impresión, abrir el documento para que el usuario pueda imprimir manualmente
                        Desktop.getDesktop().open(outputPath.toFile())
                    }
                }
                outputPath
            } else {
                null
            }
        }

        /**
         * Función para rellenar plantilla con nombre personalizado basado en el paciente
         * @param state Estado del formulario con todos los datos
         * @param templatePath Ruta de la plantilla .docx
         * @return Ruta del archivo generado o null si hubo error
         */
        fun fillTemplateForPatient(state: SessionFormState, templatePath: Path): Path? {
            val patientName = state.patient?.displayName?.replace(" ", "_")?.lowercase() ?: "paciente"
            val timestamp = Clock.System.now().toString()
                .replace(":", "-").replace(".", "-")
            val fileName = "ficha_${patientName}_${timestamp}.docx"
            
            return fillTemplateAndOpen(state, templatePath, fileName)
        }

        /**
         * Rellena el documento Word con los datos del formulario
         */
        private fun fillDocumentWithData(document: XWPFDocument, state: SessionFormState) {
            // Si es un documento nuevo, crear estructura básica
            if (document.paragraphs.size <= 1) {
                createBasicStructure(document)
            }
            
            // Buscar y reemplazar placeholders en todo el documento
            replacePlaceholders(document, state)
        }

        /**
         * Crea la estructura básica del documento si no existe plantilla
         */
        private fun createBasicStructure(document: XWPFDocument) {
            // Encabezado
            val headerPara = document.createParagraph()
            headerPara.alignment = ParagraphAlignment.CENTER
            val headerRun = headerPara.createRun()
            headerRun.setFontSize(FONT_SIZE_HEADER.toFloat())
            headerRun.isBold = true
            headerRun.setText("Elaborado por Ps. Manuel Antonio Benavente Arauco")
            
            // Subtítulo
            val subtitlePara = document.createParagraph()
            subtitlePara.alignment = ParagraphAlignment.CENTER
            val subtitleRun = subtitlePara.createRun()
            subtitleRun.setFontSize(FONT_SIZE_NORMAL.toFloat())
            subtitleRun.isBold = true
            subtitleRun.setText("NOTAS PSICOTERAPEUTICAS Y DE EVALUACIONES")
            
            // Espacio
            document.createParagraph()
            
            // Secciones básicas con placeholders
            createSectionWithPlaceholders(document, "IDENTIFICACIÓN DEL PACIENTE", listOf(
                "Apellidos y Nombres: {{NOMBRE}}",
                "Fecha de Primera Atención: {{FECHA_PRIMERA_ATENCION}}",
                "Fecha de Nacimiento: {{FECHA_NACIMIENTO}}",
                "Edad: {{EDAD}}",
                "Género: {{GENERO}}",
                "Dirección actual: {{DIRECCION}}",
                "N° DNI: {{DNI}}",
                "N° Celular: {{TELEFONO}}",
                "Motivo de consulta principal: {{MOTIVO_PRINCIPAL}}",
                "Otros motivos a tratar: {{OTROS_MOTIVOS}}"
            ))
            
            createSectionWithPlaceholders(document, "DATOS FAMILIARES DE INTERÉS", listOf(
                "{{DATOS_FAMILIARES}}"
            ))
            
            createSectionWithPlaceholders(document, "ANÁLISIS EN CADENA DE LOS PROBLEMAS PRINCIPALES", listOf(
                "{{ANALISIS_CADENA}}"
            ))
            
            createSectionWithPlaceholders(document, "Lista de metas asociadas a los problemas principales:", listOf(
                "{{METAS_PROBLEMAS}}"
            ))
            
            createSectionWithPlaceholders(document, "DATOS PSICOMÉTRICOS (DE CORRESPONDER)", listOf(
                "{{DATOS_PSICOMETRICOS}}"
            ))
            
            createSectionWithPlaceholders(document, "ÁREAS DE DESREGULACIÓN", listOf(
                "{{AREAS_DESREGULACION}}"
            ))
            
            createSectionWithPlaceholders(document, "MODELO BIOSOCIAL", listOf(
                "{{MODELO_BIOSOCIAL}}"
            ))
            
            createSectionWithPlaceholders(document, "OBJETIVOS DEL TRATAMIENTO", listOf(
                "{{OBJETIVOS_TRATAMIENTO}}"
            ))
            
            createSectionWithPlaceholders(document, "EVOLUCIÓN DE LOS OBJETIVOS", listOf(
                "{{EVOLUCION_OBJETIVOS}}"
            ))
            
            createSectionWithPlaceholders(document, "APUNTES DE EVOLUCIÓN PSICOTERAPEUTICA", listOf(
                "{{APUNTES_EVOLUCION}}"
            ))
        }

        /**
         * Crea una sección con placeholders
         */
        private fun createSectionWithPlaceholders(document: XWPFDocument, title: String, placeholders: List<String>) {
            // Título de sección
            val titlePara = document.createParagraph()
            val titleRun = titlePara.createRun()
            titleRun.setFontSize(FONT_SIZE_NORMAL.toFloat())
            titleRun.isBold = true
            titleRun.isUnderline = UnderlinePatterns.SINGLE
            titleRun.setText(title.uppercase())
            
            // Contenido con placeholders
            placeholders.forEach { placeholder ->
                val contentPara = document.createParagraph()
                val contentRun = contentPara.createRun()
                contentRun.setFontSize(FONT_SIZE_SMALL.toFloat())
                contentRun.setText(placeholder)
            }
            
            // Espacio después de la sección
            document.createParagraph()
        }

        /**
         * Reemplaza placeholders con datos reales
         */
        private fun replacePlaceholders(document: XWPFDocument, state: SessionFormState) {
            val patient = state.patient
            val session = state.session
            
            // Preparar datos de reemplazo - MAPEO COMPLETO DE TODOS LOS CAMPOS
            val replacements = mutableMapOf<String, String>()
            
            // === IDENTIFICACIÓN DEL PACIENTE ===
            replacements["{{NOMBRE_COMPLETO}}"] = patient?.displayName?.takeIf { it.isNotBlank() } ?: "[Sin nombre]"
            replacements["{{FECHA_PRIMERA_ATENCION}}"] = session?.firstAttentionDate?.toString() ?: "[Sin fecha]"
            replacements["{{FECHA_NACIMIENTO}}"] = patient?.birthDate?.toString() ?: "[Sin fecha]"
            replacements["{{EDAD}}"] = patient?.birthDate?.let { "${calculateAge(it)} años" } ?: "[Sin edad]"
            replacements["{{GENERO}}"] = patient?.gender?.takeIf { it.isNotBlank() } ?: "[Sin género]"
            replacements["{{DIRECCION}}"] = patient?.address?.takeIf { it.isNotBlank() } ?: "[Sin dirección]"
            replacements["{{DNI}}"] = patient?.dni?.takeIf { it.isNotBlank() } ?: "[Sin DNI]"
            replacements["{{TELEFONO}}"] = patient?.phone?.takeIf { it.isNotBlank() } ?: "[Sin teléfono]"
            replacements["{{MOTIVO_CONSULTA_PRINCIPAL}}"] = session?.motivoPrincipal?.takeIf { it.isNotBlank() } ?: "[Sin motivo principal]"
            replacements["{{OTROS_MOTIVOS}}"] = session?.otrosMotivos?.takeIf { it.isNotBlank() } ?: "[Sin otros motivos]"
            
            // === DATOS FAMILIARES ===
            replacements["{{DATOS_FAMILIARES}}"] = state.familyNotes?.takeIf { it.isNotBlank() } ?: "[No especificado]"
            
            // === ANÁLISIS EN CADENA ===
            state.problemChains.forEachIndexed { index, entry ->
                replacements["{{CADENA_${index + 1}_ETIQUETA}}"] = entry.label
                replacements["{{CADENA_${index + 1}_VULNERABILIDADES}}"] = entry.vulnerabilidades.orEmpty()
                replacements["{{CADENA_${index + 1}_EVENTO_DESENCADENANTE}}"] = entry.eventoDesencadenante.orEmpty()
                replacements["{{CADENA_${index + 1}_ESLABONES}}"] = entry.eslabones.orEmpty()
                replacements["{{CADENA_${index + 1}_PROBLEMAS_CONDUCTA}}"] = entry.problemasConducta.orEmpty()
                replacements["{{CADENA_${index + 1}_CONSECUENTES}}"] = entry.consecuentes.orEmpty()
            }
            
            // === METAS ASOCIADAS A PROBLEMAS ===
            replacements["{{METAS_PROBLEMAS}}"] = state.problemGoals?.metas?.takeIf { it.isNotBlank() } ?: "[No especificado]"
            
            // === DATOS PSICOMÉTRICOS ===
            val psychometrics = state.psychometrics
            replacements["{{PSICOMETRICO_COEFICIENTE_VALOR}}"] = psychometrics?.coeficienteValor.orEmpty()
            replacements["{{PSICOMETRICO_COEFICIENTE_CLASIFICACION}}"] = psychometrics?.coeficienteClasificacion.orEmpty()
            replacements["{{PSICOMETRICO_TEMPERAMENTO}}"] = psychometrics?.temperamento.orEmpty()
            replacements["{{PSICOMETRICO_PERSONALIDAD}}"] = psychometrics?.personalidad.orEmpty()
            replacements["{{PSICOMETRICO_ATENCION}}"] = psychometrics?.atencion.orEmpty()
            replacements["{{PSICOMETRICO_PROBLEMAS_CONDUCTA}}"] = psychometrics?.problemasConducta.orEmpty()
            replacements["{{PSICOMETRICO_DINAMICA_FAMILIAR}}"] = psychometrics?.dinamicaFamiliar.orEmpty()
            replacements["{{PSICOMETRICO_OTROS}}"] = psychometrics?.otrosInteres.orEmpty()
            
            // === ÁREAS DE DESREGULACIÓN ===
            val dysregulation = state.dysregulation
            replacements["{{DESEREGULACION_EMOCIONAL}}"] = dysregulation?.emocional.orEmpty()
            replacements["{{DESEREGULACION_CONDUCTUAL}}"] = dysregulation?.conductual.orEmpty()
            replacements["{{DESEREGULACION_INTERPERSONAL}}"] = dysregulation?.interpersonal.orEmpty()
            replacements["{{DESEREGULACION_SELF_VALORES}}"] = dysregulation?.selfValores.orEmpty()
            replacements["{{DESEREGULACION_COGNITIVA}}"] = dysregulation?.cognitiva.orEmpty()
            replacements["{{DESEREGULACION_RESUMEN}}"] = dysregulation?.resumen.orEmpty()
            replacements["{{DESEREGULACION_BSL23_APLICADO}}"] = if (dysregulation?.bsl23Aplicado == true) "Sí" else "No"
            
            // === MODELO BIOSOCIAL ===
            val biosocial = state.biosocial
            replacements["{{BIOSOCIAL_VULNERABILIDAD_EMOCIONAL}}"] = biosocial?.vulnerabilidadEmocional.orEmpty()
            replacements["{{BIOSOCIAL_SENSIBILIDAD}}"] = biosocial?.sensibilidad.orEmpty()
            replacements["{{BIOSOCIAL_INTENSIDAD}}"] = biosocial?.intensidad.orEmpty()
            replacements["{{BIOSOCIAL_LENTO_RETORNO_CALMA}}"] = biosocial?.lentoRetornoCalma.orEmpty()
            replacements["{{BIOSOCIAL_INVALIDACION_AMBIENTAL}}"] = biosocial?.invalidacionAmbiental.orEmpty()
            replacements["{{BIOSOCIAL_CRITICAR_EMOCIONES}}"] = biosocial?.criticarEmociones.orEmpty()
            replacements["{{BIOSOCIAL_OTROS}}"] = biosocial?.otros.orEmpty()
            
            // === OBJETIVOS DEL TRATAMIENTO ===
            val treatmentObjectives = state.treatmentObjectives.groupBy { it.stage }
            treatmentObjectives.forEach { (stage, objectives) ->
                val stagePrefix = when (stage) {
                    com.clinica.domain.model.TreatmentObjective.Stage.ETAPA_1 -> "ETAPA1"
                    com.clinica.domain.model.TreatmentObjective.Stage.ETAPA_2 -> "ETAPA2"
                    com.clinica.domain.model.TreatmentObjective.Stage.ETAPA_3 -> "ETAPA3"
                    com.clinica.domain.model.TreatmentObjective.Stage.SECUNDARIOS -> "SECUNDARIOS"
                }
                objectives.forEach { objective ->
                    val fieldKey = objective.field.replace(" ", "_").uppercase()
                    replacements["{{OBJETIVO_${stagePrefix}_${fieldKey}}}"] = objective.value
                }
            }
            
            // === EVOLUCIÓN DE OBJETIVOS (ANÁLISIS DE PROBLEMAS) ===
            state.problemAnalyses.sortedBy { it.problemNumber }.forEach { analysis ->
                val num = analysis.problemNumber
                replacements["{{PROBLEMA_${num}_DESCRIPCION}}"] = analysis.comportamiento.orEmpty()
                replacements["{{PROBLEMA_${num}_ANALISIS_SOLUCION}}"] = analysis.analisisSolucion.orEmpty()
                replacements["{{PROBLEMA_${num}_VULNERABILIDAD}}"] = analysis.vulnerabilidad.orEmpty()
                replacements["{{PROBLEMA_${num}_EVENTO_EXTERNO}}"] = analysis.eventoExterno.orEmpty()
                replacements["{{PROBLEMA_${num}_PENSAMIENTOS}}"] = analysis.pensamientos.orEmpty()
                replacements["{{PROBLEMA_${num}_SENSACIONES}}"] = analysis.sensaciones.orEmpty()
                replacements["{{PROBLEMA_${num}_IMPULSOS}}"] = analysis.impulsos.orEmpty()
                replacements["{{PROBLEMA_${num}_EMOCIONES}}"] = analysis.emociones.orEmpty()
                replacements["{{PROBLEMA_${num}_CONSECUENCIAS_INMEDIATAS}}"] = analysis.consecuenciasInmediatas.orEmpty()
                replacements["{{PROBLEMA_${num}_CONSECUENCIAS_DEMORADAS}}"] = analysis.consecuenciasDemoradas.orEmpty()
                replacements["{{PROBLEMA_${num}_PLAN_CRISIS}}"] = analysis.planCrisis.orEmpty()
            }
            
            // === APUNTES DE EVOLUCIÓN PSICOTERAPEUTICA ===
            state.evolutionNotes.forEachIndexed { index, note ->
                replacements["{{EVOLUCION_${index + 1}_TITULO}}"] = note.titulo
                replacements["{{EVOLUCION_${index + 1}_FECHA}}"] = note.notaFecha.orEmpty()
                replacements["{{EVOLUCION_${index + 1}_COMPORTAMIENTO_TRABAJADO}}"] = note.comportamientoTrabajado.orEmpty()
                replacements["{{EVOLUCION_${index + 1}_APUNTES}}"] = note.apuntes.orEmpty()
                replacements["{{EVOLUCION_${index + 1}_TAREAS}}"] = note.tareas.orEmpty()
            }
            
            // === TAREAS Y ADJUNTOS ===
            replacements["{{TAREAS_DESCRIPCION}}"] = state.tasks?.descripcion.orEmpty()
            state.attachments.forEachIndexed { index, attachment ->
                replacements["{{ADJUNTO_${index + 1}_NOMBRE}}"] = attachment.displayName
                replacements["{{ADJUNTO_${index + 1}_TAMANO}}"] = "${attachment.sizeBytes} bytes"
                replacements["{{ADJUNTO_${index + 1}_TIPO}}"] = attachment.mimeType ?: "[Desconocido]"
            }
            
            // Reemplazar en todos los párrafos
            document.paragraphs.forEach { paragraph ->
                paragraph.runs.forEach { run ->
                    var text = run.getText(0)
                    replacements.forEach { (placeholder, value) ->
                        text = text.replace(placeholder, value)
                    }
                    run.setText(text, 0)
                }
            }
        }

        /**
         * Funciones de formato para diferentes secciones
         */
        private fun formatProblemChains(chains: List<com.clinica.domain.model.ProblemChainEntry>): String {
            if (chains.isEmpty()) return "[No especificado]"
            return chains.joinToString("\n") { entry ->
                "${entry.label}: ${entry.vulnerabilidades.orEmpty()} | ${entry.eventoDesencadenante.orEmpty()} | ${entry.eslabones.orEmpty()} | ${entry.problemasConducta.orEmpty()} | ${entry.consecuentes.orEmpty()}"
            }
        }

        private fun formatPsychometrics(data: com.clinica.domain.model.PsychometricData?): String {
            data ?: return "[No especificado]"
            return buildString {
                append("Cociente intelectual: ${data.coeficienteValor.orEmpty()}\n")
                append("Dato cuantitativo: ${data.coeficienteClasificacion.orEmpty()}\n")
                append("Temperamento: ${data.temperamento.orEmpty()}\n")
                append("Personalidad: ${data.personalidad.orEmpty()}\n")
                append("Atención: ${data.atencion.orEmpty()}\n")
                append("Problemas de conducta: ${data.problemasConducta.orEmpty()}\n")
                append("Dinámica familiar: ${data.dinamicaFamiliar.orEmpty()}\n")
                append("Otros: ${data.otrosInteres.orEmpty()}")
            }
        }

        private fun formatDysregulation(data: com.clinica.domain.model.DysregulationAreas?): String {
            data ?: return "[No especificado]"
            return buildString {
                append("Emocional: ${data.emocional.orEmpty()}\n")
                append("Conductual: ${data.conductual.orEmpty()}\n")
                append("Interpersonal: ${data.interpersonal.orEmpty()}\n")
                append("Del self - valores: ${data.selfValores.orEmpty()}\n")
                append("Cognitiva: ${data.cognitiva.orEmpty()}\n")
                append("Resumen: ${data.resumen.orEmpty()}\n")
                append("BSL-23 aplicado: ${if (data.bsl23Aplicado) "Sí" else "No"}")
            }
        }

        private fun formatBiosocial(data: com.clinica.domain.model.BiosocialModel?): String {
            data ?: return "[No especificado]"
            return buildString {
                append("Vulnerabilidad emocional: ${data.vulnerabilidadEmocional.orEmpty()}\n")
                append("Sensibilidad: ${data.sensibilidad.orEmpty()}\n")
                append("Intensidad: ${data.intensidad.orEmpty()}\n")
                append("Lento retorno a la calma: ${data.lentoRetornoCalma.orEmpty()}\n")
                append("Invalidación ambiental: ${data.invalidacionAmbiental.orEmpty()}\n")
                append("Criticar emociones: ${data.criticarEmociones.orEmpty()}\n")
                append("Resumen: ${data.otros.orEmpty()}")
            }
        }

        private fun formatTreatmentObjectives(objectives: List<com.clinica.domain.model.TreatmentObjective>): String {
            if (objectives.isEmpty()) return "[No especificado]"
            return objectives.groupBy { it.stage }
                .map { (stage, stageObjectives) ->
                    val stageLabel = when (stage) {
                        com.clinica.domain.model.TreatmentObjective.Stage.ETAPA_1 -> "ETAPA I"
                        com.clinica.domain.model.TreatmentObjective.Stage.ETAPA_2 -> "ETAPA 2"
                        com.clinica.domain.model.TreatmentObjective.Stage.ETAPA_3 -> "ETAPA 3"
                        com.clinica.domain.model.TreatmentObjective.Stage.SECUNDARIOS -> "OBJETIVOS SECUNDARIOS"
                    }
                    "$stageLabel:\n" + stageObjectives.joinToString("\n") { "${it.field}: ${it.value}" }
                }
                .joinToString("\n\n")
        }

        private fun formatProblemAnalyses(analyses: List<com.clinica.domain.model.ProblemAnalysis>): String {
            if (analyses.isEmpty()) return "[No especificado]"
            return analyses.sortedBy { it.problemNumber }
                .joinToString("\n\n") { analysis ->
                    buildString {
                        append("Comportamiento problema ${analysis.problemNumber} (DFI):\n")
                        append("Análisis de la solución: ${analysis.analisisSolucion.orEmpty()}\n")
                        append("Vulnerabilidad: ${analysis.vulnerabilidad.orEmpty()}\n")
                        append("Evento precipitante externo: ${analysis.eventoExterno.orEmpty()}\n")
                        append("Pensamientos: ${analysis.pensamientos.orEmpty()}\n")
                        append("Sensaciones: ${analysis.sensaciones.orEmpty()}\n")
                        append("Impulsos: ${analysis.impulsos.orEmpty()}\n")
                        append("Emociones: ${analysis.emociones.orEmpty()}\n")
                        append("Consecuencias inmediatas: ${analysis.consecuenciasInmediatas.orEmpty()}\n")
                        append("Consecuencias demoradas: ${analysis.consecuenciasDemoradas.orEmpty()}\n")
                        append("Plan de crisis: ${analysis.planCrisis.orEmpty()}")
                    }
                }
        }

        private fun formatEvolutionNotes(notes: List<com.clinica.domain.model.EvolutionNote>): String {
            if (notes.isEmpty()) return "[No especificado]"
            return notes.joinToString("\n\n") { note ->
                buildString {
                    append("Sesión ${note.titulo}:\n")
                    append("Fecha: ${note.notaFecha.orEmpty()}\n")
                    append("Comportamiento problema trabajado: ${note.comportamientoTrabajado.orEmpty()}\n")
                    append("Apuntes: ${note.apuntes.orEmpty()}\n")
                    append("Tareas: ${note.tareas.orEmpty()}")
                }
            }
        }

        private fun calculateAge(birthDate: LocalDate): Int {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            if (birthDate > today) return 0
            return birthDate.periodUntil(today).years
        }
    }
}
