package com.clinica.desktop

import com.clinica.app.form.SessionFormState
import com.clinica.app.form.SessionFormMetadata
import com.clinica.domain.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.periodUntil
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.PDFont
import java.io.File
import java.nio.file.Path
import java.io.IOException
import kotlin.math.max

class PDFGenerator {

    companion object {
        // Constantes para el diseño del PDF
        private const val PAGE_MARGIN = 50f
        private const val FONT_SIZE_NORMAL = 10f
        private const val FONT_SIZE_SMALL = 9f
        private const val FONT_SIZE_HEADER = 12f
        private const val LINE_HEIGHT = 14f
        private const val SECTION_MARGIN = 20f
        private const val TABLE_BORDER_WIDTH = 0.5f
        
        // Fuentes
        private val FONT_NORMAL = PDType1Font.HELVETICA
        private val FONT_BOLD = PDType1Font.HELVETICA_BOLD
        private val FONT_TIMES_BOLD = PDType1Font.TIMES_BOLD
        private val FONT_TIMES_NORMAL = PDType1Font.TIMES_ROMAN

        // Coordenadas basadas en el PDF original (A4: 595 x 842 puntos)
        private val PAGE_SIZE = PDRectangle.A4
        private val PAGE_WIDTH = PAGE_SIZE.width - 2 * PAGE_MARGIN
        private val PAGE_HEIGHT = PAGE_SIZE.height - 2 * PAGE_MARGIN

        fun generatePDF(state: SessionFormState, outputPath: Path): Boolean {
            return try {
                PDDocument().use { document ->
                    val page = PDPage(PAGE_SIZE)
                    document.addPage(page)
                    
                    PDPageContentStream(document, page).use { contentStream ->
                        var currentY = PAGE_SIZE.height - PAGE_MARGIN
                        
                        // Encabezado
                        currentY = drawHeader(contentStream, currentY)
                        
                        // Secciones del formulario
                        currentY = drawIdentificationSection(contentStream, state, currentY)
                        currentY = checkPageBreak(contentStream, currentY, 100f)
                        
                        currentY = drawFamilyDataSection(contentStream, state, currentY)
                        currentY = checkPageBreak(contentStream, currentY, 200f)
                        
                        currentY = drawProblemChainSection(contentStream, state, currentY)
                        currentY = checkPageBreak(contentStream, currentY, 100f)
                        
                        currentY = drawProblemGoalsSection(contentStream, state, currentY)
                        currentY = checkPageBreak(contentStream, currentY, 150f)
                        
                        currentY = drawPsychometricsSection(contentStream, state, currentY)
                        currentY = checkPageBreak(contentStream, currentY, 150f)
                        
                        currentY = drawDysregulationSection(contentStream, state, currentY)
                        currentY = checkPageBreak(contentStream, currentY, 150f)
                        
                        currentY = drawBiosocialSection(contentStream, state, currentY)
                        currentY = checkPageBreak(contentStream, currentY, 150f)
                        
                        currentY = drawTreatmentObjectivesSection(contentStream, state, currentY)
                        currentY = checkPageBreak(contentStream, currentY, 200f)
                        
                        currentY = drawProblemAnalysisSection(contentStream, state, currentY)
                        currentY = checkPageBreak(contentStream, currentY, 200f)
                        
                        currentY = drawEvolutionNotesSection(contentStream, state, currentY)
                    }
                    
                    document.save(outputPath.toFile())
                    true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        private fun drawHeader(contentStream: PDPageContentStream, currentY: Float): Float {
            var y = currentY
            
            // Título principal
            contentStream.setFont(FONT_TIMES_BOLD, FONT_SIZE_HEADER)
            contentStream.beginText()
            contentStream.newLineAtOffset(PAGE_MARGIN, y)
            contentStream.showText("Elaborado por Ps. Manuel Antonio Benavente Arauco")
            contentStream.endText()
            y -= LINE_HEIGHT * 1.5f
            
            // Subtítulo
            contentStream.setFont(FONT_BOLD, FONT_SIZE_NORMAL)
            contentStream.beginText()
            contentStream.newLineAtOffset(PAGE_MARGIN, y)
            contentStream.showText("NOTAS PSICOTERAPEUTICAS Y DE EVALUACIONES")
            contentStream.endText()
            y -= LINE_HEIGHT * 2f
            
            return y
        }

        private fun drawIdentificationSection(contentStream: PDPageContentStream, state: SessionFormState, currentY: Float): Float {
            var y = currentY
            
            // Título de sección
            y = drawSectionTitle(contentStream, "IDENTIFICACION DEL PACIENTE", y)
            
            val patient = state.patient
            val session = state.session
            
            // Primera fila: Apellidos y Nombres + Fecha de Primera Atención
            contentStream.setFont(FONT_NORMAL, FONT_SIZE_SMALL)
            y = drawTwoColumnField(contentStream, "Apellidos y Nombres:", patient?.displayName.orEmpty(), 
                                  "Fecha de Primea Atención:", session?.firstAttentionDate?.toString().orEmpty(), y)
            
            // Segunda fila: Fecha de Nacimiento + Edad + Género
            val ageText = patient?.birthDate?.let { "${calculateAge(it)} años" } ?: ""
            y = drawThreeColumnField(contentStream, "Fecha de Nacimiento:", patient?.birthDate?.toString().orEmpty(),
                                   "Edad:", ageText, "Genero:", patient?.gender.orEmpty(), y)
            
            // Tercera fila: Dirección + DNI + Celular
            y = drawThreeColumnField(contentStream, "Dirección actual:", patient?.address.orEmpty(),
                                   "N° DNI:", patient?.dni.orEmpty(), "N° Celular:", patient?.phone.orEmpty(), y)
            
            // Campos de texto grande
            y = drawMultilineField(contentStream, "Motivo de consulta principal:", 
                                  session?.motivoPrincipal.orEmpty(), y, 3)
            y = drawMultilineField(contentStream, "Otros Motivos a tratar:", 
                                  session?.otrosMotivos.orEmpty(), y, 3)
            
            return y - SECTION_MARGIN
        }

        private fun drawFamilyDataSection(contentStream: PDPageContentStream, state: SessionFormState, currentY: Float): Float {
            var y = currentY
            
            y = drawSectionTitle(contentStream, "DATOS FAMILIARES DE INTERÉS", y)
            y = drawMultilineField(contentStream, "", state.familyNotes.orEmpty(), y, 5)
            
            return y - SECTION_MARGIN
        }

        private fun drawProblemChainSection(contentStream: PDPageContentStream, state: SessionFormState, currentY: Float): Float {
            var y = currentY
            
            y = drawSectionTitle(contentStream, "ANALISIS EN CADENA DE LOS PROBLEMAS PRINCIPALES", y)
            
            // Dibujar tabla
            val headers = listOf("Vulnerabilidades", "Evento desencadenante", "Eslabones", 
                               "Problemas de conducta o crisis", "Consecuentes")
            y = drawTableHeader(contentStream, headers, y)
            
            // Filas de problemas
            state.problemChains.forEach { entry ->
                y = drawTableRow(contentStream, listOf(
                    entry.vulnerabilidades.orEmpty(),
                    entry.eventoDesencadenante.orEmpty(),
                    entry.eslabones.orEmpty(),
                    entry.problemasConducta.orEmpty(),
                    entry.consecuentes.orEmpty()
                ), y, entry.label)
            }
            
            return y - SECTION_MARGIN
        }

        private fun drawProblemGoalsSection(contentStream: PDPageContentStream, state: SessionFormState, currentY: Float): Float {
            var y = currentY
            
            y = drawSectionTitle(contentStream, "Lista de metas asociadas a los problemas principales:", y)
            y = drawMultilineField(contentStream, "", state.problemGoals?.metas.orEmpty(), y, 4)
            
            return y - SECTION_MARGIN
        }

        private fun drawPsychometricsSection(contentStream: PDPageContentStream, state: SessionFormState, currentY: Float): Float {
            var y = currentY
            
            y = drawSectionTitle(contentStream, "DATOS PSICOMETRICOS ( DE CORRESPONDER)", y)
            
            val data = state.psychometrics
            if (data != null) {
                y = drawTwoColumnField(contentStream, "Cociente Intelectual:", data.coeficienteValor.orEmpty(),
                                      "Dato cuantitativo:", data.coeficienteClasificacion.orEmpty(), y)
                y = drawMultilineField(contentStream, "Temperamento:", data.temperamento.orEmpty(), y, 2)
                y = drawMultilineField(contentStream, "Personalidad y rasgos importantes:", data.personalidad.orEmpty(), y, 2)
                y = drawMultilineField(contentStream, "Atención y concentración:", data.atencion.orEmpty(), y, 2)
                y = drawMultilineField(contentStream, "Problemas de conducta:", data.problemasConducta.orEmpty(), y, 2)
                y = drawMultilineField(contentStream, "Dinámica Familiar:", data.dinamicaFamiliar.orEmpty(), y, 2)
                y = drawMultilineField(contentStream, "Otros de Interés:", data.otrosInteres.orEmpty(), y, 2)
            }
            
            return y - SECTION_MARGIN
        }

        private fun drawDysregulationSection(contentStream: PDPageContentStream, state: SessionFormState, currentY: Float): Float {
            var y = currentY
            
            y = drawSectionTitle(contentStream, "AREAS DE DESREGULACION", y)
            
            val data = state.dysregulation
            if (data != null) {
                y = drawMultilineField(contentStream, "Emocional:", data.emocional.orEmpty(), y, 2)
                y = drawMultilineField(contentStream, "Conductual:", data.conductual.orEmpty(), y, 2)
                y = drawMultilineField(contentStream, "Interpersonal:", data.interpersonal.orEmpty(), y, 2)
                y = drawMultilineField(contentStream, "Del Self- Valores:", data.selfValores.orEmpty(), y, 2)
                y = drawMultilineField(contentStream, "Cognitiva:", data.cognitiva.orEmpty(), y, 2)
                y = drawMultilineField(contentStream, "Resumen:", data.resumen.orEmpty(), y, 2)
                
                // Campo BSL-23
                contentStream.setFont(FONT_NORMAL, FONT_SIZE_SMALL)
                contentStream.beginText()
                contentStream.newLineAtOffset(PAGE_MARGIN, y)
                contentStream.showText("Aplicación de BsL-23 (siempre y cuando se sospeche de TLP): ${if (data.bsl23Aplicado) "Sí" else "No"}")
                contentStream.endText()
                y -= LINE_HEIGHT
            }
            
            return y - SECTION_MARGIN
        }

        private fun drawBiosocialSection(contentStream: PDPageContentStream, state: SessionFormState, currentY: Float): Float {
            var y = currentY
            
            y = drawSectionTitle(contentStream, "MODELO BIOSOCIAL", y)
            
            val data = state.biosocial
            if (data != null) {
                y = drawMultilineField(contentStream, "Vulnerabilidad Emocional:", data.vulnerabilidadEmocional.orEmpty(), y, 2)
                y = drawMultilineField(contentStream, "Sensibilidad:", data.sensibilidad.orEmpty(), y, 2)
                y = drawMultilineField(contentStream, "Intensidad:", data.intensidad.orEmpty(), y, 2)
                y = drawMultilineField(contentStream, "Lento retorno a la calma:", data.lentoRetornoCalma.orEmpty(), y, 2)
                y = drawMultilineField(contentStream, "Invalidación Ambiental:", data.invalidacionAmbiental.orEmpty(), y, 2)
                y = drawMultilineField(contentStream, "Criticar emociones:", data.criticarEmociones.orEmpty(), y, 2)
                y = drawMultilineField(contentStream, "Resumen:", data.otros.orEmpty(), y, 2)
            }
            
            return y - SECTION_MARGIN
        }

        private fun drawTreatmentObjectivesSection(contentStream: PDPageContentStream, state: SessionFormState, currentY: Float): Float {
            var y = currentY
            
            y = drawSectionTitle(contentStream, "OBJETIVOS DEL TRATAMIENTO", y)
            
            val objectives = state.treatmentObjectives
            val grouped = objectives.groupBy { it.stage }
            
            TreatmentObjective.Stage.entries.forEach { stage ->
                y = drawSubsectionTitle(contentStream, stageLabel(stage), y)
                
                val stageObjectives = grouped[stage] ?: emptyList()
                SessionFormMetadata.treatmentFields
                    .filter { it.stage == stage }
                    .forEach { field ->
                        val value = stageObjectives.firstOrNull { it.field == field.field }?.value.orEmpty()
                        y = drawMultilineField(contentStream, field.label + ":", value, y, 2)
                    }
            }
            
            return y - SECTION_MARGIN
        }

        private fun drawProblemAnalysisSection(contentStream: PDPageContentStream, state: SessionFormState, currentY: Float): Float {
            var y = currentY
            
            y = drawSectionTitle(contentStream, "EVOLUCION DE LOS OBJETIVOS", y)
            
            state.problemAnalyses.sortedBy { it.problemNumber }.forEach { analysis ->
                y = drawSubsectionTitle(contentStream, "Comportamiento Problema ${analysis.problemNumber} (DFI)", y)
                y = drawMultilineField(contentStream, "Análisis de la Solución", analysis.analisisSolucion.orEmpty(), y, 2)
                y = drawMultilineField(contentStream, "Vulnerabilidad:", analysis.vulnerabilidad.orEmpty(), y, 2)
                y = drawMultilineField(contentStream, "Evento precipitante externo:", analysis.eventoExterno.orEmpty(), y, 2)
                y = drawMultilineField(contentStream, "Pensamientos:", analysis.pensamientos.orEmpty(), y, 2)
                y = drawMultilineField(contentStream, "Sensaciones:", analysis.sensaciones.orEmpty(), y, 2)
                y = drawMultilineField(contentStream, "Impulsos:", analysis.impulsos.orEmpty(), y, 2)
                y = drawMultilineField(contentStream, "Emociones:", analysis.emociones.orEmpty(), y, 2)
                y = drawMultilineField(contentStream, "Consecuencias inmediatas reforzantes:", analysis.consecuenciasInmediatas.orEmpty(), y, 2)
                y = drawMultilineField(contentStream, "Consecuencias demoradas:", analysis.consecuenciasDemoradas.orEmpty(), y, 2)
                y = drawMultilineField(contentStream, "Resuma el Plan de Crisis:", analysis.planCrisis.orEmpty(), y, 2)
            }
            
            return y - SECTION_MARGIN
        }

        private fun drawEvolutionNotesSection(contentStream: PDPageContentStream, state: SessionFormState, currentY: Float): Float {
            var y = currentY
            
            y = drawSectionTitle(contentStream, "APUNTES DE EVOLUCION PSICOTERAPEUTICA", y)
            
            state.evolutionNotes.forEach { note ->
                y = drawSubsectionTitle(contentStream, "Sesión ${note.titulo}", y)
                y = drawTwoColumnField(contentStream, "Fecha:", note.notaFecha.orEmpty(),
                                      "Comportamiento problema Trabajado:", note.comportamientoTrabajado.orEmpty(), y)
                y = drawMultilineField(contentStream, "Apuntes:", note.apuntes.orEmpty(), y, 3)
                y = drawMultilineField(contentStream, "Tareas:", note.tareas.orEmpty(), y, 3)
            }
            
            return y
        }

        // Métodos de utilidad para dibujar elementos comunes
        
        private fun drawSectionTitle(contentStream: PDPageContentStream, title: String, currentY: Float): Float {
            contentStream.setFont(FONT_BOLD, FONT_SIZE_NORMAL)
            contentStream.beginText()
            contentStream.newLineAtOffset(PAGE_MARGIN, currentY)
            contentStream.showText(title.uppercase())
            contentStream.endText()
            return currentY - LINE_HEIGHT * 1.5f
        }

        private fun drawSubsectionTitle(contentStream: PDPageContentStream, title: String, currentY: Float): Float {
            contentStream.setFont(FONT_BOLD, FONT_SIZE_SMALL)
            contentStream.beginText()
            contentStream.newLineAtOffset(PAGE_MARGIN, currentY)
            contentStream.showText(title)
            contentStream.endText()
            return currentY - LINE_HEIGHT * 1.2f
        }

        private fun drawTwoColumnField(contentStream: PDPageContentStream, label1: String, value1: String, 
                                     label2: String, value2: String, currentY: Float): Float {
            var y = currentY
            
            contentStream.setFont(FONT_NORMAL, FONT_SIZE_SMALL)
            
            // Primera columna
            contentStream.beginText()
            contentStream.newLineAtOffset(PAGE_MARGIN, y)
            contentStream.showText(label1)
            contentStream.endText()
            
            contentStream.beginText()
            contentStream.newLineAtOffset(PAGE_MARGIN + 80f, y)
            contentStream.showText(value1.take(30))
            contentStream.endText()
            
            // Segunda columna
            contentStream.beginText()
            contentStream.newLineAtOffset(PAGE_MARGIN + 250f, y)
            contentStream.showText(label2)
            contentStream.endText()
            
            contentStream.beginText()
            contentStream.newLineAtOffset(PAGE_MARGIN + 350f, y)
            contentStream.showText(value2.take(20))
            contentStream.endText()
            
            return y - LINE_HEIGHT
        }

        private fun drawThreeColumnField(contentStream: PDPageContentStream, label1: String, value1: String,
                                        label2: String, value2: String, label3: String, value3: String, currentY: Float): Float {
            var y = currentY
            
            contentStream.setFont(FONT_NORMAL, FONT_SIZE_SMALL)
            
            // Primera columna
            contentStream.beginText()
            contentStream.newLineAtOffset(PAGE_MARGIN, y)
            contentStream.showText(label1)
            contentStream.endText()
            
            contentStream.beginText()
            contentStream.newLineAtOffset(PAGE_MARGIN + 70f, y)
            contentStream.showText(value1.take(15))
            contentStream.endText()
            
            // Segunda columna
            contentStream.beginText()
            contentStream.newLineAtOffset(PAGE_MARGIN + 180f, y)
            contentStream.showText(label2)
            contentStream.endText()
            
            contentStream.beginText()
            contentStream.newLineAtOffset(PAGE_MARGIN + 220f, y)
            contentStream.showText(value2.take(10))
            contentStream.endText()
            
            // Tercera columna
            contentStream.beginText()
            contentStream.newLineAtOffset(PAGE_MARGIN + 300f, y)
            contentStream.showText(label3)
            contentStream.endText()
            
            contentStream.beginText()
            contentStream.newLineAtOffset(PAGE_MARGIN + 360f, y)
            contentStream.showText(value3.take(15))
            contentStream.endText()
            
            return y - LINE_HEIGHT
        }

        private fun drawMultilineField(contentStream: PDPageContentStream, label: String, value: String, currentY: Float, lines: Int): Float {
            var y = currentY
            
            if (label.isNotEmpty()) {
                contentStream.setFont(FONT_NORMAL, FONT_SIZE_SMALL)
                contentStream.beginText()
                contentStream.newLineAtOffset(PAGE_MARGIN, y)
                contentStream.showText(label)
                contentStream.endText()
                y -= LINE_HEIGHT
            }
            
            if (value.isNotEmpty()) {
                val words = value.split(" ")
                var currentLine = ""
                var lineCount = 0
                
                words.forEach { word ->
                    if (currentLine.length + word.length + 1 > 80) {
                        if (lineCount < lines) {
                            contentStream.beginText()
                            contentStream.newLineAtOffset(PAGE_MARGIN + (if (label.isNotEmpty()) 20f else 0f), y)
                            contentStream.showText(currentLine)
                            contentStream.endText()
                            y -= LINE_HEIGHT
                            lineCount++
                        }
                        currentLine = word
                    } else {
                        currentLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                    }
                }
                
                if (currentLine.isNotEmpty() && lineCount < lines) {
                    contentStream.beginText()
                    contentStream.newLineAtOffset(PAGE_MARGIN + (if (label.isNotEmpty()) 20f else 0f), y)
                    contentStream.showText(currentLine)
                    contentStream.endText()
                    y -= LINE_HEIGHT
                }
            }
            
            return y
        }

        private fun drawTableHeader(contentStream: PDPageContentStream, headers: List<String>, currentY: Float): Float {
            var y = currentY
            val columnWidths = listOf(0.35f, 1f, 1f, 1f, 1f)
            var currentX = PAGE_MARGIN
            
            // Dibujar bordes de la tabla
            contentStream.setLineWidth(TABLE_BORDER_WIDTH)
            
            // Línea superior
            contentStream.moveTo(PAGE_MARGIN, y)
            contentStream.lineTo(PAGE_MARGIN + columnWidths.sum() * 80f, y)
            contentStream.stroke()
            
            // Encabezados
            contentStream.setFont(FONT_BOLD, FONT_SIZE_SMALL)
            headers.forEachIndexed { index, header ->
                contentStream.beginText()
                contentStream.newLineAtOffset(currentX + 5f, y - 5f)
                contentStream.showText(header)
                contentStream.endText()
                
                // Líneas verticales
                contentStream.moveTo(currentX, y)
                contentStream.lineTo(currentX, y - 100f)
                contentStream.stroke()
                
                currentX += columnWidths[index] * 80f
            }
            
            // Última línea vertical
            contentStream.moveTo(currentX, y)
            contentStream.lineTo(currentX, y - 100f)
            contentStream.stroke()
            
            return y - 15f
        }

        private fun drawTableRow(contentStream: PDPageContentStream, values: List<String>, currentY: Float, label: String): Float {
            var y = currentY
            val columnWidths = listOf(0.35f, 1f, 1f, 1f, 1f)
            var currentX = PAGE_MARGIN
            
            // Etiqueta de la fila
            contentStream.setFont(FONT_BOLD, FONT_SIZE_SMALL)
            contentStream.beginText()
            contentStream.newLineAtOffset(currentX + 5f, y - 5f)
            contentStream.showText(label)
            contentStream.endText()
            
            // Contenido de las celdas
            contentStream.setFont(FONT_NORMAL, FONT_SIZE_SMALL)
            currentX += columnWidths[0] * 80f
            
            values.forEachIndexed { index, value ->
                val truncatedValue = value.take(30)
                contentStream.beginText()
                contentStream.newLineAtOffset(currentX + 5f, y - 5f)
                contentStream.showText(truncatedValue)
                contentStream.endText()
                currentX += columnWidths[index + 1] * 80f
            }
            
            // Línea inferior de la fila
            contentStream.moveTo(PAGE_MARGIN, y - 20f)
            contentStream.lineTo(PAGE_MARGIN + columnWidths.sum() * 80f, y - 20f)
            contentStream.stroke()
            
            return y - 25f
        }

        private fun checkPageBreak(contentStream: PDPageContentStream, currentY: Float, requiredSpace: Float): Float {
            return if (currentY - requiredSpace < PAGE_MARGIN) {
                // Aquí se debería crear una nueva página, por ahora seguimos en la misma
                currentY
            } else {
                currentY
            }
        }

        private fun stageLabel(stage: TreatmentObjective.Stage): String = when (stage) {
            TreatmentObjective.Stage.ETAPA_1 -> "ETAPA I"
            TreatmentObjective.Stage.ETAPA_2 -> "ETAPA 2"
            TreatmentObjective.Stage.ETAPA_3 -> "ETAPA 3"
            TreatmentObjective.Stage.SECUNDARIOS -> "OBJETIVOS SECUNDARIOS"
        }

        private fun calculateAge(birthDate: LocalDate): Int {
            val today = kotlinx.datetime.Clock.System.todayIn(TimeZone.currentSystemDefault())
            if (birthDate > today) return 0
            return birthDate.periodUntil(today).years
        }
    }
}
