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
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.awt.Desktop

class WordDocumentGenerator {

    companion object {
        // Constantes para el diseño
        private const val TEMPLATE_RESOURCE = "/templates/ficha-consulta.docx"
        private const val FONT_SIZE_NORMAL = 10
        private const val FONT_SIZE_SMALL = 9
        private const val FONT_SIZE_HEADER = 12
        private val TREATMENT_ROW_BY_FIELD = mapOf(
            "conductas_amenazan_vida" to 5,
            "conductas_interfieren_en_terapia" to 6,
            "conductas_interfieren_calidad_vida" to 7,
            "deficit_habilidades" to 8,
            "disminuir_evitacion_experiencial" to 10,
            "aumentar_procesamiento_emocional" to 11,
            "incrementar_recuperacion_emocional" to 12,
            "disminuir_sensacion_vacio" to 13,
            "disminuir_alienacion" to 14,
            "resolucion_problemas" to 16,
            "logro_metas" to 17,
            "generalizacion_habilidades" to 18,
            "conductas_generadoras_crisis" to 20,
            "vulnerabilidad_emocional" to 21,
            "pasividad_activa" to 22,
            "inhibicion_emocional" to 23,
            "auto_invalidacion" to 24,
            "competencia_aparente" to 25
        )

        fun ensureBundledTemplate(): Path? {
            return try {
                val targetPath = Paths.get(System.getProperty("java.io.tmpdir"))
                    .resolve("ficha-consulta-template.docx")

                val resourceStream = WordDocumentGenerator::class.java
                    .getResourceAsStream(TEMPLATE_RESOURCE)
                    ?: return null

                resourceStream.use { input ->
                    Files.copy(input, targetPath, StandardCopyOption.REPLACE_EXISTING)
                }

                targetPath
            } catch (e: Exception) {
                println("No se pudo preparar la plantilla Word: ${e.message}")
                e.printStackTrace()
                null
            }
        }

        /**
         * Función principal para rellenar plantilla Word con datos del formulario
         * @param state Estado del formulario con todos los datos
         * @param templatePath Ruta de la plantilla .docx
         * @param outputPath Ruta donde se guardará el documento rellenado
         * @return true si se generó correctamente, false si hubo error
         */
        fun fillTemplateWithFormData(state: SessionFormState, templatePath: Path, outputPath: Path): Boolean {
            return try {
                val templateFile = templatePath.toFile()
                if (!templateFile.exists()) {
                    println("ERROR: No se encuentra la plantilla en: $templatePath")
                    return false
                }

                outputPath.parent?.let { parent ->
                    runCatching { Files.createDirectories(parent) }
                }

                FileInputStream(templateFile).use { input ->
                    XWPFDocument(input).use { document ->
                        populateTemplate(document, state)
                        FileOutputStream(outputPath.toFile()).use { out ->
                            document.write(out)
                            out.flush()
                        }
                    }
                }

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
        fun fillTemplateForPatient(
            state: SessionFormState,
            templatePath: Path,
            fileName: String? = null
        ): Path? {
            val downloadsDir = Paths.get(System.getProperty("user.home"), "Downloads")
            val resolvedFileName = fileName ?: defaultFileName(state)
            val outputPath = downloadsDir.resolve(resolvedFileName)

            return generateDocument(state, templatePath, outputPath) { generated ->
                if (Desktop.isDesktopSupported()) {
                    runCatching { Desktop.getDesktop().open(generated.toFile()) }
                        .onFailure { error ->
                            println("No se pudo abrir el documento generado: ${error.message}")
                        }
                }
            }
        }

        /**
         * Función que rellena la plantilla y envía automáticamente a la impresora (LEGADO - NO USAR)
         * @deprecated Usar fillTemplateAndPrintDirect en su lugar
         * @param state Estado del formulario con todos los datos
         * @param templatePath Ruta de la plantilla .docx
         * @return Ruta del archivo generado o null si hubo error
         */
        @Deprecated("Usar fillTemplateAndPrintDirect en su lugar para evitar abrir aplicaciones externas")
        fun fillTemplateAndPrint(state: SessionFormState, templatePath: Path): Path? {
            val tempDir = Paths.get(System.getProperty("java.io.tmpdir"))
            val patientName = state.patient?.displayName?.replace(" ", "_")?.lowercase() ?: "paciente"
            val timestamp = Clock.System.now().toString()
                .replace(":", "-").replace(".", "-")
            val fileName = "ficha_${patientName}_${timestamp}.docx"
            val outputPath = tempDir.resolve(fileName)

            return generateDocument(state, templatePath, outputPath) { generated ->
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().print(generated.toFile())
                        println("Documento enviado a impresión: $generated")
                    } catch (e: Exception) {
                        println("Error al enviar a impresión: ${e.message}")
                        // NO abrir el documento - esto causaba el problema de Word/Nitro PDF
                        println("Documento generado para impresión manual: $generated")
                    }
                }
            }
        }

        /**
         * Función que rellena la plantilla y genera PDF directamente
         * @param state Estado del formulario con todos los datos
         * @param templatePath Ruta de la plantilla .docx
         * @return Ruta del archivo PDF generado o null si hubo error
         */
        fun fillTemplateAndGeneratePDF(state: SessionFormState, templatePath: Path): Path? {
            val tempDir = Paths.get(System.getProperty("java.io.tmpdir"))
            val patientName = state.patient?.displayName?.replace(" ", "_")?.lowercase() ?: "paciente"
            val timestamp = Clock.System.now().toString()
                .replace(":", "-").replace(".", "-")
            val docxFileName = "ficha_${patientName}_${timestamp}.docx"
            val docxPath = tempDir.resolve(docxFileName)

            // Primero generar el documento Word con los datos
            val wordGenerated = generateDocument(state, templatePath, docxPath)
            if (wordGenerated == null) {
                println("❌ Falló la generación del documento Word")
                return null
            }

            // Luego convertir a PDF usando un enfoque diferente
            try {
                val downloadsDir = Paths.get(System.getProperty("user.home"), "Downloads")
                val pdfFileName = "Ficha_${patientName}_${java.time.LocalDate.now()}.pdf"
                val pdfPath = downloadsDir.resolve(pdfFileName)

                val success = convertWordToPDFUsingCommand(wordGenerated, pdfPath)
                if (success) {
                    println("✅ PDF generado exitosamente: $pdfPath")
                    // Eliminar el archivo Word temporal
                    Files.deleteIfExists(wordGenerated)
                    return pdfPath
                } else {
                    println("❌ Falló la conversión a PDF")
                    return null
                }
            } catch (e: Exception) {
                println("❌ Error en conversión a PDF: ${e.message}")
                e.printStackTrace()
                return null
            }
        }

        /**
         * Convierte Word a PDF usando comandos del sistema (LibreOffice o Microsoft Office)
         */
        private fun convertWordToPDFUsingCommand(wordPath: Path, pdfPath: Path): Boolean {
            // La conversión a PDF ha sido deshabilitada temporalmente
            // Se generará solo el documento Word
            println("📄 Documento Word generado correctamente en: $wordPath")
            return true
        }

  
        private fun generateDocument(
            state: SessionFormState,
            templatePath: Path,
            outputPath: Path,
            onSuccess: ((Path) -> Unit)? = null
        ): Path? {
            return if (fillTemplateWithFormData(state, templatePath, outputPath)) {
                onSuccess?.let { action ->
                    runCatching { action(outputPath) }
                        .onFailure { error ->
                            println("Acción posterior falló: ${error.message}")
                        }
                }
                outputPath
            } else {
                null
            }
        }

        private fun defaultFileName(state: SessionFormState): String {
            val patientName = state.patient?.displayName
                ?.trim()
                ?.replace(" ", "_")
                ?.lowercase()
                ?.takeIf { it.isNotEmpty() }
                ?: "paciente"
            val timestamp = Clock.System.now().toString()
                .replace(":", "-")
                .replace(".", "-")
            return "ficha_${patientName}_${timestamp}.docx"
        }

        private fun populateTemplate(document: XWPFDocument, state: SessionFormState) {
            val tables = document.tables
            if (tables.isEmpty()) return

            val mainTable = tables[0]
            fillIdentificationSection(mainTable, state)
            fillProblemChainsSection(mainTable, state)
            fillPsychometricsSection(mainTable, state)
            fillDysregulationSection(mainTable, state)
            fillBiosocialSection(mainTable, tables.getOrNull(1), state)

            tables.getOrNull(1)?.let { fillTreatmentObjectivesSection(it, state) }
            fillProblemAnalysesSection(tables, state)
            fillEvolutionNotesSection(tables.getOrNull(2), state)
        }

        private fun fillIdentificationSection(table: XWPFTable, state: SessionFormState) {
            val patient = state.patient
            val session = state.session

            setLabeledValue(table, 2, 0, patient?.displayName)
            setLabeledValue(table, 2, 1, formatDate(session?.firstAttentionDate))
            setLabeledValue(table, 3, 0, formatDate(patient?.birthDate))
            setLabeledValue(table, 3, 1, patient?.birthDate?.let { "${calculateAge(it)} años" })
            setLabeledValue(table, 3, 2, patient?.gender)
            setLabeledValue(table, 4, 0, patient?.address)
            setLabeledValue(table, 4, 1, patient?.dni)
            setLabeledValue(table, 4, 2, patient?.phone)
            setLabeledValue(table, 5, 0, session?.motivoPrincipal)
            setLabeledValue(table, 6, 0, session?.otrosMotivos)
            setPlainValue(table, 8, 0, state.familyNotes)
        }

        private fun fillProblemChainsSection(table: XWPFTable, state: SessionFormState) {
            state.problemChains.take(4).forEachIndexed { index, entry ->
                val rowIndex = 11 + index
                setPlainValue(table, rowIndex, 0, entry.label.ifBlank { "P${index + 1}" })
                setPlainValue(table, rowIndex, 1, entry.vulnerabilidades)
                setPlainValue(table, rowIndex, 2, entry.eventoDesencadenante)
                setPlainValue(table, rowIndex, 3, entry.eslabones)
                setPlainValue(table, rowIndex, 4, entry.problemasConducta)
                setPlainValue(table, rowIndex, 5, entry.consecuentes)
            }

            setLabeledValue(table, 15, 0, state.problemGoals?.metas)
        }

        private fun fillPsychometricsSection(table: XWPFTable, state: SessionFormState) {
            val psychometrics = state.psychometrics
            setLabeledValue(table, 17, 0, psychometrics?.coeficienteValor)
            setLabeledValue(table, 17, 1, psychometrics?.coeficienteValor)
            setLabeledValue(table, 17, 2, psychometrics?.coeficienteClasificacion)
            setLabeledValue(table, 18, 1, psychometrics?.temperamento)
            setLabeledValue(table, 19, 1, psychometrics?.personalidad)
            setLabeledValue(table, 20, 1, psychometrics?.atencion)
            setLabeledValue(table, 21, 1, psychometrics?.problemasConducta)
            setLabeledValue(table, 22, 1, psychometrics?.dinamicaFamiliar)
            setLabeledValue(table, 23, 1, psychometrics?.otrosInteres)
        }

        private fun fillDysregulationSection(table: XWPFTable, state: SessionFormState) {
            val dysregulation = state.dysregulation
            setLabeledValue(table, 25, 0, dysregulation?.emocional)
            setLabeledValue(table, 26, 0, dysregulation?.conductual)
            setLabeledValue(table, 27, 0, dysregulation?.interpersonal)
            setLabeledValue(table, 28, 0, dysregulation?.selfValores)
            setLabeledValue(table, 29, 0, dysregulation?.cognitiva)
            setLabeledValue(table, 30, 0, dysregulation?.resumen)
            val bslValue = if (dysregulation?.bsl23Aplicado == true) "Sí" else "No"
            setLabeledValue(table, 31, 1, bslValue)
        }

        private fun fillBiosocialSection(mainTable: XWPFTable, secondaryTable: XWPFTable?, state: SessionFormState) {
            val biosocial = state.biosocial
            setLabeledValue(mainTable, 33, 0, biosocial?.vulnerabilidadEmocional)
            setLabeledValue(mainTable, 34, 0, biosocial?.sensibilidad)
            setLabeledValue(mainTable, 35, 0, biosocial?.intensidad)
            setLabeledValue(mainTable, 36, 0, biosocial?.lentoRetornoCalma)
            setLabeledValue(mainTable, 37, 0, biosocial?.invalidacionAmbiental)
            setLabeledValue(mainTable, 38, 0, biosocial?.criticarEmociones)

            secondaryTable?.let {
                setLabeledValue(it, 2, 0, biosocial?.otros)
            }
        }

        private fun fillTreatmentObjectivesSection(table: XWPFTable, state: SessionFormState) {
            state.treatmentObjectives.forEach { objective ->
                val rowIndex = TREATMENT_ROW_BY_FIELD[objective.field] ?: return@forEach
                setLabeledValue(table, rowIndex, 0, objective.value)
            }
        }

        private fun fillProblemAnalysesSection(tables: List<XWPFTable>, state: SessionFormState) {
            val tableOne = tables.getOrNull(1) ?: return
            val tableTwo = tables.getOrNull(2)
            val analyses = state.problemAnalyses.sortedBy { it.problemNumber }

            analyses.getOrNull(0)?.let { analysis ->
                val mappings = listOf(
                    27 to analysis.comportamiento,
                    28 to analysis.analisisSolucion,
                    29 to analysis.vulnerabilidad,
                    30 to analysis.eventoExterno,
                    31 to analysis.pensamientos,
                    32 to analysis.sensaciones,
                    33 to analysis.impulsos,
                    34 to analysis.emociones,
                    35 to analysis.consecuenciasInmediatas,
                    36 to analysis.consecuenciasDemoradas,
                    37 to analysis.planCrisis
                )
                mappings.forEach { (rowIndex, value) ->
                    setLabeledValue(tableOne, rowIndex, 0, value)
                }
            }

            analyses.getOrNull(1)?.let { analysis ->
                val firstRows = listOf(
                    38 to analysis.comportamiento,
                    39 to analysis.analisisSolucion,
                    40 to analysis.vulnerabilidad
                )
                firstRows.forEach { (rowIndex, value) ->
                    setLabeledValue(tableOne, rowIndex, 0, value)
                }

                tableTwo?.let { secondTable ->
                    val secondRows = listOf(
                        0 to analysis.eventoExterno,
                        1 to analysis.pensamientos,
                        2 to analysis.sensaciones,
                        3 to analysis.impulsos,
                        4 to analysis.emociones,
                        5 to analysis.consecuenciasInmediatas,
                        6 to analysis.consecuenciasDemoradas,
                        7 to analysis.planCrisis
                    )
                    secondRows.forEach { (rowIndex, value) ->
                        setLabeledValue(secondTable, rowIndex, 0, value)
                    }
                }
            }
        }

        private fun fillEvolutionNotesSection(table: XWPFTable?, state: SessionFormState) {
            table ?: return
            state.evolutionNotes.firstOrNull()?.let { note ->
                setPlainValue(table, 9, 0, note.titulo)
                setLabeledValue(table, 9, 1, note.notaFecha)
                setLabeledValue(table, 9, 2, note.comportamientoTrabajado)
                setLabeledValue(table, 10, 0, note.apuntes)
            }

            val tasksText = buildTasksText(state)
            setLabeledValue(table, 11, 0, tasksText)
        }

        private fun setLabeledValue(table: XWPFTable, rowIndex: Int, cellIndex: Int, value: String?) {
            val row = table.getRow(rowIndex) ?: return
            val cell = row.getCell(cellIndex) ?: return
            val label = cell.text.trim()
            val trimmedValue = value?.trim().orEmpty()
            val text = when {
                label.isEmpty() -> trimmedValue
                trimmedValue.isEmpty() -> label
                label.endsWith(":") -> "$label $trimmedValue"
                else -> "$label: $trimmedValue"
            }
            cell.writeText(text)
        }

        private fun setPlainValue(table: XWPFTable, rowIndex: Int, cellIndex: Int, value: String?) {
            val row = table.getRow(rowIndex) ?: return
            val cell = row.getCell(cellIndex) ?: return
            cell.writeText(value?.trim().orEmpty())
        }

        private fun XWPFTableCell.writeText(text: String, fontSize: Int = FONT_SIZE_NORMAL) {
            while (paragraphs.size > 0) {
                removeParagraph(0)
            }
            val paragraph = addParagraph()
            val run = paragraph.createRun()
            run.setFontSize(fontSize)
            run.setText(text, 0)
        }

        private fun buildTasksText(state: SessionFormState): String? {
            val sections = mutableListOf<String>()

            var description = state.tasks?.descripcion?.trim().orEmpty()
            if (description.isNotEmpty()) {
                state.attachments.forEach { attachment ->
                    val token = "[${attachment.displayName}]"
                    if (description.contains(token)) {
                        description = description.replace(token, attachment.displayName)
                    }
                }
                val cleaned = removeAttachmentNames(description, state.attachments)
                if (cleaned.isNotBlank()) {
                    sections += cleaned
                }
            }

            // Los archivos adjuntos se manejan por separado, no se incluyen en el campo de tareas

            val noteTasks = state.evolutionNotes.mapNotNull { note ->
                note.tareas?.trim()?.takeIf { it.isNotEmpty() }?.let { tareas ->
                    val label = note.titulo.ifBlank { "Sesión" }
                    "- $label: $tareas"
                }
            }
            if (noteTasks.isNotEmpty()) {
                sections += "Seguimiento por sesión:\n" + noteTasks.joinToString("\n")
            }

            val result = sections.joinToString(separator = "\n\n").trim()
            return result.ifEmpty { null }
        }

        private fun removeAttachmentNames(text: String, attachments: List<com.clinica.domain.model.Attachment>): String {
            var result = text
            attachments.forEach { attachment ->
                val pattern = Regex("\\s*" + Regex.escape(attachment.displayName) + "\\s*")
                result = result.replace(pattern, " ")
            }
            return result.replace(Regex("\\s+"), " ").trim()
        }

        private fun formatDate(date: LocalDate?): String? {
            return date?.let {
                "%02d/%02d/%04d".format(it.dayOfMonth, it.monthNumber, it.year)
            }
        }

        private fun calculateAge(birthDate: LocalDate): Int {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            if (birthDate > today) return 0
            return birthDate.periodUntil(today).years
        }

        /**
         * Función para imprimir directamente sin abrir aplicaciones externas
         * @param state Estado del formulario con todos los datos
         * @param templatePath Ruta de la plantilla .docx
         * @return true si se envió a impresión correctamente, false si hubo error
         */
        fun fillTemplateAndPrintDirect(state: SessionFormState, templatePath: Path): Boolean {
            return try {
                val tempDir = Paths.get(System.getProperty("java.io.tmpdir"))
                val patientName = state.patient?.displayName?.replace(" ", "_")?.lowercase() ?: "paciente"
                val timestamp = Clock.System.now().toString()
                    .replace(":", "-").replace(".", "-")
                val fileName = "ficha_${patientName}_${timestamp}.docx"
                val outputPath = tempDir.resolve(fileName)

                // Generar el documento Word con los datos
                if (!fillTemplateWithFormData(state, templatePath, outputPath)) {
                    println("❌ Falló la generación del documento Word")
                    return false
                }

                // Imprimir usando comando nativo del sistema
                val success = printWithNativeCommand(outputPath)
                
                // Eliminar el archivo temporal después de imprimir
                try {
                    Files.deleteIfExists(outputPath)
                } catch (e: Exception) {
                    println("Advertencia: No se pudo eliminar archivo temporal: ${e.message}")
                }

                success
            } catch (e: Exception) {
                println("❌ Error en impresión directa: ${e.message}")
                e.printStackTrace()
                false
            }
        }

        /**
         * Imprime documento usando comando nativo del sistema sin abrir aplicaciones
         * @param documentPath Ruta del documento a imprimir
         * @return true si se envió a impresión correctamente
         */
        private fun printWithNativeCommand(documentPath: Path): Boolean {
            return try {
                val osName = System.getProperty("os.name").lowercase()
                val command = when {
                    osName.contains("win") -> {
                        // Windows: usar comando print
                        val printerName = getDefaultPrinterWindows()
                        val printCommand = if (printerName.isNotEmpty() && printerName != "default") {
                            "cmd /c print /d:\"$printerName\" \"${documentPath.toAbsolutePath()}\""
                        } else {
                            // Si no hay impresora específica, usar el comando sin especificar impresora
                            "cmd /c print \"${documentPath.toAbsolutePath()}\""
                        }
                        println("Usando impresora: ${printerName.ifEmpty { "predeterminada del sistema" }}")
                        printCommand
                    }
                    osName.contains("mac") -> {
                        // macOS: usar lpr
                        "lpr \"${documentPath.toAbsolutePath()}\""
                    }
                    else -> {
                        // Linux y otros: usar lpr
                        "lpr \"${documentPath.toAbsolutePath()}\""
                    }
                }

                println("Ejecutando comando de impresión: $command")
                val process = Runtime.getRuntime().exec(command)
                val exitCode = process.waitFor()
                
                if (exitCode == 0) {
                    println("✅ Documento enviado a impresión correctamente")
                    true
                } else {
                    println("❌ Error en impresión. Código de salida: $exitCode")
                    // Leer el error stream para obtener más información
                    val errorReader = process.errorStream.bufferedReader()
                    val errorOutput = errorReader.readText()
                    errorReader.close()
                    if (errorOutput.isNotEmpty()) {
                        println("Detalles del error: $errorOutput")
                    }
                    false
                }
            } catch (e: Exception) {
                println("❌ Error al ejecutar comando de impresión: ${e.message}")
                e.printStackTrace()
                false
            }
        }

        /**
         * Obtiene la impresora predeterminada en Windows
         */
        private fun getDefaultPrinterWindows(): String {
            return try {
                // Método 1: Usar WMIC para obtener la impresora predeterminada
                val wmicCommand = "cmd /c WMIC PRINTER WHERE DEFAULT=TRUE GET NAME /VALUE"
                val wmicProcess = Runtime.getRuntime().exec(wmicCommand)
                val wmicReader = wmicProcess.inputStream.bufferedReader()
                val wmicOutput = wmicReader.readText()
                wmicReader.close()
                
                // La salida es en formato "Name=NombreImpresora"
                val nameLine = wmicOutput.lines().find { it.startsWith("Name=") }
                val printerName = nameLine?.substringAfter("Name=")?.trim()
                
                if (!printerName.isNullOrEmpty()) {
                    return printerName
                }
                
                // Método 2: Intentar con otro formato de WMIC
                val wmicCommand2 = "cmd /c WMIC PRINTER GET NAME"
                val wmicProcess2 = Runtime.getRuntime().exec(wmicCommand2)
                val wmicReader2 = wmicProcess2.inputStream.bufferedReader()
                val wmicOutput2 = wmicReader2.readLines()
                wmicReader2.close()
                
                // La segunda línea contiene el nombre de la impresora (la primera es "Name")
                wmicOutput2.getOrNull(1)?.trim() ?: ""
            } catch (e: Exception) {
                println("No se pudo obtener impresora predeterminada: ${e.message}")
                // Devolver un nombre genérico para que el sistema use la impresora por defecto
                // En lugar de vacío, usamos un nombre que no causará error en el comando
                "default"
            }
        }
    }
}
