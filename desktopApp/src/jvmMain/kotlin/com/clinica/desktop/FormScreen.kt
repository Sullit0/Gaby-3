package com.clinica.desktop

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import kotlinx.coroutines.delay
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.awt.ComposeWindow
import com.clinica.app.form.SessionFormMetadata
import com.clinica.app.form.SessionFormState
import com.clinica.app.form.SessionFormViewModel
import com.clinica.domain.model.Attachment
import com.clinica.domain.model.BiosocialModel
import com.clinica.domain.model.DysregulationAreas
import com.clinica.domain.model.PsychometricData
import com.clinica.domain.model.TreatmentObjective
import com.benasher44.uuid.uuid4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.periodUntil
import kotlinx.datetime.atStartOfDayIn
import org.koin.core.context.GlobalContext
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Point
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.DEFAULT_BUFFER_SIZE
import kotlinx.datetime.toLocalDateTime
import androidx.compose.ui.unit.toSize

@Composable
fun SessionFormScreen(
    modifier: Modifier = Modifier,
    storageRoot: Path,
    composeWindow: ComposeWindow? = null,
    selectedPatientId: String? = null
) {
    val koin = GlobalContext.get()
    val viewModel = remember {
        SessionFormViewModel(
            patientRepository = koin.get(),
            sessionRepository = koin.get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        )
    }
    DisposableEffect(Unit) {
        if (selectedPatientId != null) {
            viewModel.loadPatientById(selectedPatientId)
        } else {
            viewModel.loadDefaultPatient()
        }
        onDispose { viewModel.dispose() }
    }

    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val dropBoundsRef = remember { AtomicReference<Rect?>(null) }
    var isDragHovering by remember { mutableStateOf(false) }

    DesktopDropTargetHandler(
        composeWindow = composeWindow,
        boundsRef = dropBoundsRef,
        onHoverChange = { isDragHovering = it },
        onFilesDropped = { files ->
            val patientId = state.patient?.id
            val sessionId = state.session?.id
            if (patientId == null || sessionId == null) return@DesktopDropTargetHandler
            coroutineScope.launch(Dispatchers.IO) {
                val created = processAttachments(files.toTypedArray(), storageRoot, patientId, sessionId)
                created.forEach { attachment ->
                    viewModel.registerAttachment(attachment)
                    viewModel.appendAttachmentToken(attachment)
                }
            }
        }
    )

    if (state.isLoading) {
        Column(
            modifier = modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Text("Cargando ficha...")
        }
        return
    }

    state.error?.let { errorMessage ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Error: $errorMessage",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(vertical = 24.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        SectionCard(title = "Identificación del paciente") {
            IdentificationSection(state, viewModel)
        }

        SectionCard(title = "Datos familiares de interés") {
            ControlledLargeTextField(
                label = "Descripción",
                value = state.familyNotes,
                onValueChange = { viewModel.updateFamilyNotes(it) },
                placeholder = "Historia familiar, vínculos, hitos relevantes"
            )
        }

        SectionCard(title = "Análisis en cadena de los problemas principales") {
            ProblemChainSection(state, viewModel)
            Spacer(modifier = Modifier.height(12.dp))
            ControlledLargeTextField(
                label = "Lista de metas asociadas a los problemas principales",
                value = state.problemGoals?.metas.orEmpty(),
                onValueChange = { viewModel.updateProblemGoals(it) }
            )
        }

        SectionCard(title = "Datos psicométricos (de corresponder)") {
            PsychometricsSection(state, viewModel)
        }

        SectionCard(title = "Áreas de desregulación") {
            DysregulationSection(state, viewModel)
        }

        SectionCard(title = "Modelo biosocial") {
            BiosocialSection(state, viewModel)
        }

        SectionCard(title = "Objetivos del tratamiento") {
            TreatmentObjectivesSection(state, viewModel)
        }

        SectionCard(title = "Evolución de los objetivos") {
            ProblemAnalysisSection(state, viewModel)
        }

        SectionCard(title = "Apuntes de evolución psicoterapéutica") {
            EvolutionNotesSection(state, viewModel)
        }

        SectionCard(
            title = "Tareas / Adjuntos de la sesión",
            actions = {
                IconButton(
                    onClick = {
                        val patientId = state.patient?.id ?: return@IconButton
                        val sessionId = state.session?.id ?: return@IconButton
                        val files = chooseFiles() ?: return@IconButton
                        coroutineScope.launch(Dispatchers.IO) {
                            val created = processAttachments(files, storageRoot, patientId, sessionId)
                            created.forEach { attachment ->
                                viewModel.registerAttachment(attachment)
                                viewModel.appendAttachmentToken(attachment)
                            }
                        }
                    },
                    enabled = state.patient != null && state.session != null
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Adjuntar archivo")
                }
            }
        ) {
            TasksSection(
                state = state,
                viewModel = viewModel,
                storageRoot = storageRoot,
                dropBoundsRef = dropBoundsRef,
                isDragHovering = isDragHovering
            )
        }

        // Botón de guardar al final
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                var isSaving by remember { mutableStateOf(false) }
                    var saveSuccess by remember { mutableStateOf(false) }

                    Button(
                    onClick = {
                        coroutineScope.launch {
                            isSaving = true
                            saveSuccess = false
                            viewModel.saveSession()
                            // Pequeña espera para asegurar que se guarde
                            kotlinx.coroutines.delay(500)
                            isSaving = false
                            saveSuccess = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.5f),
                    enabled = state.patient != null && state.session != null && !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Guardando...")
                    } else {
                        Text(if (saveSuccess) "✓ Guardado" else "Guardar Ficha")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (actions == null) Arrangement.Start else Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                if (actions != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun IdentificationSection(state: SessionFormState, viewModel: SessionFormViewModel) {
    val patient = state.patient
    val session = state.session

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = patient?.displayName.orEmpty(),
            onValueChange = { viewModel.updatePatientName(it) },
            label = { Text("Apellidos y nombres") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            DatePickerField(
                label = "Fecha de nacimiento",
                value = patient?.birthDate,
                onValueChange = { viewModel.updateBirthDate(it) },
                modifier = Modifier.weight(1f)
            )
            AgeDisplay(modifier = Modifier.weight(1f), birthDate = patient?.birthDate)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            DatePickerField(
                label = "Fecha de primera atención",
                value = session?.firstAttentionDate,
                onValueChange = { viewModel.updateFirstAttentionDate(it) },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = patient?.gender.orEmpty(),
                onValueChange = { viewModel.updateGender(it) },
                label = { Text("Género") },
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = patient?.address.orEmpty(),
                onValueChange = { viewModel.updateDireccion(it) },
                label = { Text("Dirección actual") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = patient?.dni.orEmpty(),
                onValueChange = { viewModel.updateDni(it) },
                label = { Text("Nº DNI") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = patient?.phone.orEmpty(),
                onValueChange = { viewModel.updatePhone(it) },
                label = { Text("Nº Celular") },
                modifier = Modifier.weight(1f)
            )
        }
        ControlledLargeTextField(
            label = "Motivo de consulta principal",
            value = session?.motivoPrincipal.orEmpty(),
            onValueChange = { viewModel.updateMotivoPrincipal(it) }
        )
        ControlledLargeTextField(
            label = "Otros motivos a tratar",
            value = session?.otrosMotivos.orEmpty(),
            onValueChange = { viewModel.updateOtrosMotivos(it) }
        )
    }
}

@Composable
private fun ProblemChainSection(state: SessionFormState, viewModel: SessionFormViewModel) {
    val chains = state.problemChains
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        chains.forEach { entry ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium)
                    )
                    ControlledLargeTextField(
                        label = "Vulnerabilidades",
                        value = entry.vulnerabilidades.orEmpty(),
                        onValueChange = { value ->
                            viewModel.updateProblemChain(entry.label) { it.copy(vulnerabilidades = value) }
                        }
                    )
                    ControlledLargeTextField(
                        label = "Evento desencadenante",
                        value = entry.eventoDesencadenante.orEmpty(),
                        onValueChange = { value ->
                            viewModel.updateProblemChain(entry.label) { it.copy(eventoDesencadenante = value) }
                        }
                    )
                    ControlledLargeTextField(
                        label = "Eslabones",
                        value = entry.eslabones.orEmpty(),
                        onValueChange = { value ->
                            viewModel.updateProblemChain(entry.label) { it.copy(eslabones = value) }
                        }
                    )
                    ControlledLargeTextField(
                        label = "Problemas de conducta o crisis",
                        value = entry.problemasConducta.orEmpty(),
                        onValueChange = { value ->
                            viewModel.updateProblemChain(entry.label) { it.copy(problemasConducta = value) }
                        }
                    )
                    ControlledLargeTextField(
                        label = "Consecuentes",
                        value = entry.consecuentes.orEmpty(),
                        onValueChange = { value ->
                            viewModel.updateProblemChain(entry.label) { it.copy(consecuentes = value) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PsychometricsSection(state: SessionFormState, viewModel: SessionFormViewModel) {
    val data = state.psychometrics ?: defaultPsychometricsStub(state.session?.id)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            ControlledLargeTextField(
                label = "Cociente intelectual - dato cuantitativo",
                value = data.coeficienteValor.orEmpty(),
                onValueChange = { value ->
                    viewModel.updatePsychometrics { it.copy(coeficienteValor = value) }
                },
                modifier = Modifier.weight(1f),
                minHeight = 80.dp
            )
            ControlledLargeTextField(
                label = "Cociente intelectual - clasificación",
                value = data.coeficienteClasificacion.orEmpty(),
                onValueChange = { value ->
                    viewModel.updatePsychometrics { it.copy(coeficienteClasificacion = value) }
                },
                modifier = Modifier.weight(1f),
                minHeight = 80.dp
            )
        }
        ControlledLargeTextField(
            label = "Temperamento",
            value = data.temperamento.orEmpty(),
            onValueChange = { value -> viewModel.updatePsychometrics { it.copy(temperamento = value) } }
        )
        ControlledLargeTextField(
            label = "Personalidad y rasgos importantes",
            value = data.personalidad.orEmpty(),
            onValueChange = { value -> viewModel.updatePsychometrics { it.copy(personalidad = value) } }
        )
        ControlledLargeTextField(
            label = "Atención y concentración",
            value = data.atencion.orEmpty(),
            onValueChange = { value -> viewModel.updatePsychometrics { it.copy(atencion = value) } }
        )
        ControlledLargeTextField(
            label = "Problemas de conducta",
            value = data.problemasConducta.orEmpty(),
            onValueChange = { value -> viewModel.updatePsychometrics { it.copy(problemasConducta = value) } }
        )
        ControlledLargeTextField(
            label = "Dinámica familiar",
            value = data.dinamicaFamiliar.orEmpty(),
            onValueChange = { value -> viewModel.updatePsychometrics { it.copy(dinamicaFamiliar = value) } }
        )
        ControlledLargeTextField(
            label = "Otros de interés",
            value = data.otrosInteres.orEmpty(),
            onValueChange = { value -> viewModel.updatePsychometrics { it.copy(otrosInteres = value) } }
        )
    }
}

@Composable
private fun DysregulationSection(state: SessionFormState, viewModel: SessionFormViewModel) {
    val data = state.dysregulation ?: defaultDysregulationStub(state.session?.id)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ControlledLargeTextField(
            label = "Emocional",
            value = data.emocional.orEmpty(),
            onValueChange = { value -> viewModel.updateDysregulation { it.copy(emocional = value) } }
        )
        ControlledLargeTextField(
            label = "Conductual",
            value = data.conductual.orEmpty(),
            onValueChange = { value -> viewModel.updateDysregulation { it.copy(conductual = value) } }
        )
        ControlledLargeTextField(
            label = "Interpersonal",
            value = data.interpersonal.orEmpty(),
            onValueChange = { value -> viewModel.updateDysregulation { it.copy(interpersonal = value) } }
        )
        ControlledLargeTextField(
            label = "Del self - valores",
            value = data.selfValores.orEmpty(),
            onValueChange = { value -> viewModel.updateDysregulation { it.copy(selfValores = value) } }
        )
        ControlledLargeTextField(
            label = "Cognitiva",
            value = data.cognitiva.orEmpty(),
            onValueChange = { value -> viewModel.updateDysregulation { it.copy(cognitiva = value) } }
        )
        ControlledLargeTextField(
            label = "Resumen",
            value = data.resumen.orEmpty(),
            onValueChange = { value -> viewModel.updateDysregulation { it.copy(resumen = value) } }
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Aplicación de BsL-23 (siempre y cuando se sospeche de TLP)")
            Switch(
                checked = data.bsl23Aplicado,
                onCheckedChange = { checked -> viewModel.updateDysregulation { it.copy(bsl23Aplicado = checked) } }
            )
        }
    }
}

@Composable
private fun BiosocialSection(state: SessionFormState, viewModel: SessionFormViewModel) {
    val data = state.biosocial ?: defaultBiosocialStub(state.session?.id)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ControlledLargeTextField(
            label = "Vulnerabilidad emocional",
            value = data.vulnerabilidadEmocional.orEmpty(),
            onValueChange = { value -> viewModel.updateBiosocial { it.copy(vulnerabilidadEmocional = value) } }
        )
        ControlledLargeTextField(
            label = "Sensibilidad",
            value = data.sensibilidad.orEmpty(),
            onValueChange = { value -> viewModel.updateBiosocial { it.copy(sensibilidad = value) } }
        )
        ControlledLargeTextField(
            label = "Intensidad",
            value = data.intensidad.orEmpty(),
            onValueChange = { value -> viewModel.updateBiosocial { it.copy(intensidad = value) } }
        )
        ControlledLargeTextField(
            label = "Lento retorno a la calma",
            value = data.lentoRetornoCalma.orEmpty(),
            onValueChange = { value -> viewModel.updateBiosocial { it.copy(lentoRetornoCalma = value) } }
        )
        ControlledLargeTextField(
            label = "Invalidación ambiental",
            value = data.invalidacionAmbiental.orEmpty(),
            onValueChange = { value -> viewModel.updateBiosocial { it.copy(invalidacionAmbiental = value) } }
        )
        ControlledLargeTextField(
            label = "Criticar emociones",
            value = data.criticarEmociones.orEmpty(),
            onValueChange = { value -> viewModel.updateBiosocial { it.copy(criticarEmociones = value) } }
        )
        ControlledLargeTextField(
            label = "Otros",
            value = data.otros.orEmpty(),
            onValueChange = { value -> viewModel.updateBiosocial { it.copy(otros = value) } }
        )
    }
}

@Composable
private fun TreatmentObjectivesSection(state: SessionFormState, viewModel: SessionFormViewModel) {
    val objectives = state.treatmentObjectives
    val grouped = SessionFormMetadata.treatmentFields.groupBy { it.stage }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        grouped.forEach { (stage, definitions) ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stageLabel(stage),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium)
                )
                definitions.forEach { definition ->
                    val value = objectives.firstOrNull { it.stage == stage && it.field == definition.field }?.value.orEmpty()
                    ControlledLargeTextField(
                        label = definition.label,
                        value = value,
                        onValueChange = { text -> viewModel.updateTreatmentObjective(stage, definition.field, text) }
                    )
                }
            }
            if (stage != grouped.keys.last()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun ProblemAnalysisSection(state: SessionFormState, viewModel: SessionFormViewModel) {
    val analyses = state.problemAnalyses.sortedBy { it.problemNumber }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        analyses.forEach { analysis ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Comportamiento problema ${analysis.problemNumber} (DFI)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium)
                    )
                    ControlledLargeTextField(
                        label = "Descripción",
                        value = analysis.comportamiento.orEmpty(),
                        onValueChange = { value ->
                            viewModel.updateProblemAnalysis(analysis.problemNumber) { it.copy(comportamiento = value) }
                        }
                    )
                    ControlledLargeTextField(
                        label = "Análisis de la solución",
                        value = analysis.analisisSolucion.orEmpty(),
                        onValueChange = { value ->
                            viewModel.updateProblemAnalysis(analysis.problemNumber) { it.copy(analisisSolucion = value) }
                        }
                    )
                    ControlledLargeTextField(
                        label = "Vulnerabilidad",
                        value = analysis.vulnerabilidad.orEmpty(),
                        onValueChange = { value ->
                            viewModel.updateProblemAnalysis(analysis.problemNumber) { it.copy(vulnerabilidad = value) }
                        }
                    )
                    ControlledLargeTextField(
                        label = "Evento precipitante externo",
                        value = analysis.eventoExterno.orEmpty(),
                        onValueChange = { value ->
                            viewModel.updateProblemAnalysis(analysis.problemNumber) { it.copy(eventoExterno = value) }
                        }
                    )
                    ControlledLargeTextField(
                        label = "Pensamientos",
                        value = analysis.pensamientos.orEmpty(),
                        onValueChange = { value ->
                            viewModel.updateProblemAnalysis(analysis.problemNumber) { it.copy(pensamientos = value) }
                        }
                    )
                    ControlledLargeTextField(
                        label = "Sensaciones",
                        value = analysis.sensaciones.orEmpty(),
                        onValueChange = { value ->
                            viewModel.updateProblemAnalysis(analysis.problemNumber) { it.copy(sensaciones = value) }
                        }
                    )
                    ControlledLargeTextField(
                        label = "Impulsos",
                        value = analysis.impulsos.orEmpty(),
                        onValueChange = { value ->
                            viewModel.updateProblemAnalysis(analysis.problemNumber) { it.copy(impulsos = value) }
                        }
                    )
                    ControlledLargeTextField(
                        label = "Emociones",
                        value = analysis.emociones.orEmpty(),
                        onValueChange = { value ->
                            viewModel.updateProblemAnalysis(analysis.problemNumber) { it.copy(emociones = value) }
                        }
                    )
                    ControlledLargeTextField(
                        label = "Consecuencias inmediatas reforzantes",
                        value = analysis.consecuenciasInmediatas.orEmpty(),
                        onValueChange = { value ->
                            viewModel.updateProblemAnalysis(analysis.problemNumber) { it.copy(consecuenciasInmediatas = value) }
                        }
                    )
                    ControlledLargeTextField(
                        label = "Consecuencias demoradas",
                        value = analysis.consecuenciasDemoradas.orEmpty(),
                        onValueChange = { value ->
                            viewModel.updateProblemAnalysis(analysis.problemNumber) { it.copy(consecuenciasDemoradas = value) }
                        }
                    )
                    ControlledLargeTextField(
                        label = "Resuma el plan de crisis",
                        value = analysis.planCrisis.orEmpty(),
                        onValueChange = { value ->
                            viewModel.updateProblemAnalysis(analysis.problemNumber) { it.copy(planCrisis = value) }
                        }
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { viewModel.removeProblemAnalysis(analysis.problemNumber) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Eliminar problema")
                        }
                        Text(
                            text = "#${analysis.problemNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }
        }
        Button(onClick = { viewModel.addProblemAnalysis() }) {
            Text("Agregar comportamiento problema")
        }
    }
}

@Composable
private fun EvolutionNotesSection(state: SessionFormState, viewModel: SessionFormViewModel) {
    val notes = state.evolutionNotes
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        notes.forEach { note ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = note.titulo,
                            onValueChange = { value -> viewModel.updateEvolutionNote(note.id) { it.copy(titulo = value) } },
                            label = { Text("Sesión") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(12.dp))
                        OutlinedTextField(
                            value = note.notaFecha.orEmpty(),
                            onValueChange = { value -> viewModel.updateEvolutionNote(note.id) { it.copy(notaFecha = value) } },
                            label = { Text("Fecha") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    ControlledLargeTextField(
                        label = "Comportamiento problema trabajado",
                        value = note.comportamientoTrabajado.orEmpty(),
                        onValueChange = { value -> viewModel.updateEvolutionNote(note.id) { it.copy(comportamientoTrabajado = value) } }
                    )
                    ControlledLargeTextField(
                        label = "Apuntes",
                        value = note.apuntes.orEmpty(),
                        onValueChange = { value -> viewModel.updateEvolutionNote(note.id) { it.copy(apuntes = value) } }
                    )
                    ControlledLargeTextField(
                        label = "Tareas",
                        value = note.tareas.orEmpty(),
                        onValueChange = { value -> viewModel.updateEvolutionNote(note.id) { it.copy(tareas = value) } }
                    )
                    TextButton(onClick = { viewModel.removeEvolutionNote(note.id) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Eliminar registro")
                    }
                }
            }
        }
        Button(onClick = { viewModel.addEvolutionNote() }) {
            Text("Agregar sesión")
        }
    }
}

@Composable
private fun TasksSection(
    state: SessionFormState,
    viewModel: SessionFormViewModel,
    storageRoot: Path,
    dropBoundsRef: AtomicReference<Rect?>,
    isDragHovering: Boolean
) {
    val patientId = state.patient?.id
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AttachmentTasksField(
            value = state.tasks?.descripcion.orEmpty(),
            attachments = state.attachments,
            onValueChange = { viewModel.updateTasks(it.takeIf { text -> text.isNotBlank() }) },
            isDragHovering = isDragHovering,
            dropBoundsRef = dropBoundsRef,
            onTokenClick = { attachment ->
                val path = patientId?.let { pid ->
                    storageRoot.resolve(pid)
                        .resolve(attachment.sessionId)
                        .resolve(attachment.storedName)
                }
                if (path != null && Files.exists(path)) {
                    openAttachment(path)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentTasksField(
    value: String,
    attachments: List<Attachment>,
    onValueChange: (String) -> Unit,
    isDragHovering: Boolean,
    dropBoundsRef: AtomicReference<Rect?>,
    onTokenClick: (Attachment) -> Unit
) {
    var fieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var innerTextOffset by remember { mutableStateOf(Offset.Zero) }
    var innerTextSize by remember { mutableStateOf(Size.Zero) }
    var containerPosition by remember { mutableStateOf(Offset.Zero) }
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            fieldValue = fieldValue.copy(text = value, selection = TextRange(value.length))
        }
    }

    val highlightStyle = SpanStyle(
        background = MaterialTheme.colorScheme.secondaryContainer,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        fontWeight = FontWeight.Medium
    )
    val tokenTransformation = remember(highlightStyle) { TokenHighlightTransformation(highlightStyle) }
    val borderColor = if (isDragHovering) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = borderColor,
        unfocusedBorderColor = borderColor,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary
    )

    data class TokenSpan(val range: IntRange, val attachment: Attachment)

    val tokenSpans = remember(fieldValue.text, attachments) {
        "\\[([^\\]]+)]".toRegex().findAll(fieldValue.text).mapNotNull { match ->
            val displayName = match.groupValues.getOrNull(1) ?: return@mapNotNull null
            val attachment = attachments.firstOrNull { it.displayName == displayName }
            if (attachment != null) TokenSpan(match.range, attachment) else null
        }.toList()
    }

    BasicTextField(
        value = fieldValue,
        onValueChange = {
            fieldValue = it
            onValueChange(it.text)
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp)
            .onGloballyPositioned { coordinates ->
                dropBoundsRef.set(coordinates.boundsInWindow())
                containerPosition = coordinates.positionInRoot()
            }
            .pointerInput(tokenSpans, innerTextOffset, innerTextSize, textLayoutResult) {
                if (tokenSpans.isEmpty()) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(pass = PointerEventPass.Initial)
                    val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                    if (up.pressed) return@awaitEachGesture
                    val layout = textLayoutResult ?: return@awaitEachGesture
                    val size = innerTextSize
                    if (size.width <= 0f || size.height <= 0f) return@awaitEachGesture
                    val position = up.position - innerTextOffset
                    val clamped = Offset(
                        x = position.x.coerceIn(0f, size.width),
                        y = position.y.coerceIn(0f, size.height)
                    )
                    val offset = layout.getOffsetForPosition(clamped)
                    tokenSpans.firstOrNull { offset in it.range }?.let { onTokenClick(it.attachment) }
                }
            },
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        visualTransformation = tokenTransformation,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        interactionSource = interactionSource,
        maxLines = Int.MAX_VALUE,
        onTextLayout = { textLayoutResult = it }
    ) { innerTextField ->
        OutlinedTextFieldDefaults.DecorationBox(
            value = fieldValue.text,
            visualTransformation = tokenTransformation,
            innerTextField = {
                Box(
                    Modifier.onGloballyPositioned {
                        innerTextOffset = it.positionInRoot() - containerPosition
                        innerTextSize = it.size.toSize()
                    }
                ) {
                    innerTextField()
                }
            },
            placeholder = { Text("Describe las tareas y añade tags como [archivo.pdf]") },
            label = { Text("Descripción de tareas") },
            singleLine = false,
            enabled = true,
            isError = false,
            interactionSource = interactionSource,
            colors = colors
        )
    }
}

private class TokenHighlightTransformation(
    private val style: SpanStyle,
    private val regex: Regex = "\\[[^\\]]+\\]".toRegex()
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text.text)
        regex.findAll(text.text).forEach { match ->
            builder.addStyle(style, match.range.first, match.range.last + 1)
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

@Composable
private fun ControlledLargeTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    minHeight: Dp = 120.dp
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        textStyle = MaterialTheme.typography.bodyMedium,
        maxLines = Int.MAX_VALUE
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    label: String,
    value: LocalDate?,
    onValueChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val textValue = value?.let { formatDate(it) }.orEmpty()

    OutlinedTextField(
        value = textValue,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Outlined.DateRange, contentDescription = "Seleccionar fecha")
            }
        },
        modifier = modifier
    )

    if (showDialog) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = value?.toEpochMillis())
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = datePickerState.selectedDateMillis?.toLocalDate()
                    onValueChange(selected)
                    showDialog = false
                }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onValueChange(null)
                    showDialog = false
                }) {
                    Text("Limpiar")
                }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }
}

@Composable
private fun AgeDisplay(modifier: Modifier, birthDate: LocalDate?) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Edad",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        val ageText = birthDate?.let { "${calculateAge(it)} años" } ?: "—"
        Text(
            text = ageText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DesktopDropTargetHandler(
    composeWindow: ComposeWindow?,
    boundsRef: AtomicReference<Rect?>,
    onHoverChange: (Boolean) -> Unit,
    onFilesDropped: (List<File>) -> Unit
) {
    val scope = rememberCoroutineScope()

    DisposableEffect(composeWindow) {
        val window = composeWindow ?: return@DisposableEffect onDispose { }
        val component = window.contentPane
        val originalTarget = component.dropTarget
        val listener = object : DropTargetAdapter() {
            override fun dragEnter(dtde: DropTargetDragEvent) = handleDrag(dtde)
            override fun dragOver(dtde: DropTargetDragEvent) = handleDrag(dtde)
            override fun dragExit(dte: DropTargetEvent) {
                scope.launch { onHoverChange(false) }
            }

            override fun drop(dtde: DropTargetDropEvent) {
                val rect = boundsRef.get()
                rect?.containsPoint(dtde.location, tolerance = 64f)
                scope.launch { onHoverChange(false) }
                dtde.acceptDrop(DnDConstants.ACTION_COPY)
                val files = extractFiles(dtde.transferable)
                dtde.dropComplete(true)
                if (files.isNotEmpty()) {
                    scope.launch { onFilesDropped(files) }
                }
            }

            private fun handleDrag(event: DropTargetDragEvent) {
                val rect = boundsRef.get()
                val inside = rect?.containsPoint(event.location, tolerance = 64f) ?: true
                event.acceptDrag(DnDConstants.ACTION_COPY)
                scope.launch { onHoverChange(inside) }
            }
        }

        val dropTarget = DropTarget(component, DnDConstants.ACTION_COPY, listener, true)
        component.dropTarget = dropTarget
        onDispose {
            dropTarget.setComponent(null)
            component.dropTarget = originalTarget
            scope.launch { onHoverChange(false) }
        }
    }
}

private fun Rect.containsPoint(point: Point, tolerance: Float = 0f): Boolean {
    val leftBound = left - tolerance
    val rightBound = right + tolerance
    val topBound = top - tolerance
    val bottomBound = bottom + tolerance
    val x = point.x.toFloat()
    val y = point.y.toFloat()
    return x in leftBound..rightBound && y in topBound..bottomBound
}

private fun extractFiles(transferable: Transferable): List<File> {
    return try {
        when {
            transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor) -> {
                val data = transferable.getTransferData(DataFlavor.javaFileListFlavor)
                if (data is List<*>) data.filterIsInstance<File>() else emptyList()
            }
            else -> emptyList()
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun stageLabel(stage: TreatmentObjective.Stage): String = when (stage) {
    TreatmentObjective.Stage.ETAPA_1 -> "Etapa 1"
    TreatmentObjective.Stage.ETAPA_2 -> "Etapa 2"
    TreatmentObjective.Stage.ETAPA_3 -> "Etapa 3"
    TreatmentObjective.Stage.SECUNDARIOS -> "Objetivos secundarios"
}

fun calculateAge(birthDate: LocalDate): Int {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    if (birthDate > today) return 0
    return birthDate.periodUntil(today).years
}

private fun chooseFiles(): Array<File>? {
    val dialog = FileDialog(null as Frame?, "Seleccionar archivos", FileDialog.LOAD)
    dialog.isMultipleMode = true
    dialog.isVisible = true
    val files = dialog.files
    dialog.dispose()
    return if (files.isNotEmpty()) files else null
}

private fun processAttachments(
    files: Array<File>,
    storageRoot: Path,
    patientId: String,
    sessionId: String
): List<Attachment> {
    val sessionFolder = storageRoot.resolve(patientId).resolve(sessionId)
    Files.createDirectories(sessionFolder)
    val created = mutableListOf<Attachment>()
    files.forEach { file ->
        if (!file.exists()) return@forEach
        val storedName = "${uuid4()}_${file.name}"
        val target = sessionFolder.resolve(storedName)
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(file.toPath()).use { input ->
            Files.newOutputStream(target).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var read = input.read(buffer)
                while (read != -1) {
                    output.write(buffer, 0, read)
                    digest.update(buffer, 0, read)
                    read = input.read(buffer)
                }
            }
        }
        val hash = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
        val size = Files.size(target)
        val mime = Files.probeContentType(target)
        val attachment = Attachment(
            id = uuid4().toString(),
            sessionId = sessionId,
            displayName = file.name,
            storedName = storedName,
            mimeType = mime,
            sizeBytes = size,
            sha256 = hash,
            createdAt = Clock.System.now()
        )
        created.add(attachment)
    }
    return created
}

private fun openAttachment(path: Path) {
    if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().open(path.toFile())
    }
}

fun formatDate(date: LocalDate): String = date.toString()

private fun LocalDate.toEpochMillis(): Long =
    this.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

private fun Long.toLocalDate(): LocalDate =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date

private fun defaultPsychometricsStub(sessionId: String?): PsychometricData =
    PsychometricData(
        sessionId = sessionId.orEmpty(),
        coeficienteValor = null,
        coeficienteClasificacion = null,
        temperamento = null,
        personalidad = null,
        atencion = null,
        problemasConducta = null,
        dinamicaFamiliar = null,
        otrosInteres = null
    )

private fun defaultDysregulationStub(sessionId: String?): DysregulationAreas =
    DysregulationAreas(
        sessionId = sessionId.orEmpty(),
        emocional = null,
        conductual = null,
        interpersonal = null,
        selfValores = null,
        cognitiva = null,
        resumen = null,
        bsl23Aplicado = false
    )

private fun defaultBiosocialStub(sessionId: String?) =
    BiosocialModel(
        sessionId = sessionId.orEmpty(),
        vulnerabilidadEmocional = null,
        sensibilidad = null,
        intensidad = null,
        lentoRetornoCalma = null,
        invalidacionAmbiental = null,
        criticarEmociones = null,
        otros = null
    )
