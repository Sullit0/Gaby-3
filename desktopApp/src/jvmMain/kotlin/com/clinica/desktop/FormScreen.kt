package com.clinica.desktop

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
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
import kotlin.math.max
import kotlin.math.roundToInt
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

    LaunchedEffect(selectedPatientId) {
        if (selectedPatientId != null) {
            viewModel.loadPatientById(selectedPatientId)
        } else {
            viewModel.loadDefaultPatient()
        }
    }

    DisposableEffect(Unit) {
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
    if (state.patient == null || state.session == null) {
        EmptySessionPlaceholder(modifier)
        return
    }

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(vertical = 24.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        SectionCard(title = "IdentificaciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n del paciente") {
            IdentificationSection(state, viewModel)
        }

        SectionCard(title = "Datos familiares de interÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©s") {
            ControlledLargeTextField(
                label = "DescripciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n",
                value = state.familyNotes,
                onValueChange = { viewModel.updateFamilyNotes(it) },
                placeholder = "Historia familiar, vÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â­nculos, hitos relevantes"
            )
        }

        SectionCard(title = "AnÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡lisis en cadena de los problemas principales") {
            ProblemChainSection(state, viewModel)
            Spacer(modifier = Modifier.height(12.dp))
            ControlledLargeTextField(
                label = "Lista de metas asociadas a los problemas principales",
                value = state.problemGoals?.metas.orEmpty(),
                onValueChange = { viewModel.updateProblemGoals(it) }
            )
        }

        SectionCard(title = "Datos psicomÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©tricos (de corresponder)") {
            PsychometricsSection(state, viewModel)
        }

        SectionCard(title = "ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Âreas de desregulaciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n") {
            DysregulationSection(state, viewModel)
        }

        SectionCard(title = "Modelo biosocial") {
            BiosocialSection(state, viewModel)
        }

        SectionCard(title = "Objetivos del tratamiento") {
            TreatmentObjectivesSection(state, viewModel)
        }

        SectionCard(title = "EvoluciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n de los objetivos") {
            ProblemAnalysisSection(state, viewModel)
        }

        SectionCard(title = "Apuntes de evoluciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n psicoterapÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©utica") {
            EvolutionNotesSection(state, viewModel)
        }

        SectionCard(
            title = "Tareas / Adjuntos de la sesiÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n",
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

        // BotÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n de guardar al final
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
                            // PequeÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â±a espera para asegurar que se guarde
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
                        Text(if (saveSuccess) "ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã¢â‚¬Å“ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œ Guardado" else "Guardar Ficha")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySessionPlaceholder(modifier: Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Selecciona un paciente para cargar la ficha",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Crea un paciente desde la gestiÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n o elige uno existente para comenzar a completar la ficha.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
private fun SectionCard(
    title: String,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .border(1.dp, Color.Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF424242))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = if (actions == null) Arrangement.Center else Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
            if (actions != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
        }
    }
}
@Composable
private fun IdentificationSection(state: SessionFormState, viewModel: SessionFormViewModel) {
    val patient = state.patient
    val session = state.session

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            FormTextField(
                label = "Apellidos y Nombres",
                value = patient?.displayName.orEmpty(),
                onValueChange = { viewModel.updatePatientName(it) },
                modifier = Modifier.weight(2f)
            )
            FormDateField(
                label = "Fecha de Primera AtenciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n",
                value = session?.firstAttentionDate,
                onValueChange = { viewModel.updateFirstAttentionDate(it) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            FormDateField(
                label = "Fecha de Nacimiento",
                value = patient?.birthDate,
                onValueChange = { viewModel.updateBirthDate(it) },
                modifier = Modifier.weight(1f)
            )
            FormReadOnlyField(
                label = "Edad",
                value = patient?.birthDate?.let { "${calculateAge(it)} aÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â±os" } ?: "",
                modifier = Modifier.weight(0.7f)
            )
            FormTextField(
                label = "GÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©nero",
                value = patient?.gender.orEmpty(),
                onValueChange = { viewModel.updateGender(it) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            FormTextField(
                label = "DirecciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n actual",
                value = patient?.address.orEmpty(),
                onValueChange = { viewModel.updateDireccion(it) },
                modifier = Modifier.weight(2f)
            )
            FormTextField(
                label = "NÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â° DNI",
                value = patient?.dni.orEmpty(),
                onValueChange = { viewModel.updateDni(it) },
                modifier = Modifier.weight(1f)
            )
            FormTextField(
                label = "NÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â° Celular",
                value = patient?.phone.orEmpty(),
                onValueChange = { viewModel.updatePhone(it) },
                modifier = Modifier.weight(1f)
            )
        }
        FormTextField(
            label = "Motivo de consulta principal",
            value = session?.motivoPrincipal.orEmpty(),
            onValueChange = { viewModel.updateMotivoPrincipal(it) },
            minLines = 3
        )
        FormTextField(
            label = "Otros motivos a tratar",
            value = session?.otrosMotivos.orEmpty(),
            onValueChange = { viewModel.updateOtrosMotivos(it) },
            minLines = 3
        )
    }
}

@Composable
private fun ProblemChainSection(state: SessionFormState, viewModel: SessionFormViewModel) {
    val chains = state.problemChains
    val headers = listOf(
        "Vulnerabilidades",
        "Evento desencadenante",
        "Eslabones",
        "Problemas de conducta o crisis",
        "Consecuentes"
    )

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Black),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            TableHeaderCell(text = "", modifier = Modifier.weight(0.35f))
            headers.forEach { header ->
                TableHeaderCell(text = header, modifier = Modifier.weight(1f))
            }
        }
        chains.forEach { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Black),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                FormCell(
                    label = "",
                    modifier = Modifier.weight(0.35f),
                    backgroundColor = Color(0xFFF5F5F5)
                ) {
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                FormTextField(
                    label = "",
                    value = entry.vulnerabilidades.orEmpty(),
                    onValueChange = { value ->
                        viewModel.updateProblemChain(entry.label) { it.copy(vulnerabilidades = value) }
                    },
                    minLines = 3,
                    modifier = Modifier.weight(1f)
                )
                FormTextField(
                    label = "",
                    value = entry.eventoDesencadenante.orEmpty(),
                    onValueChange = { value ->
                        viewModel.updateProblemChain(entry.label) { it.copy(eventoDesencadenante = value) }
                    },
                    minLines = 3,
                    modifier = Modifier.weight(1f)
                )
                FormTextField(
                    label = "",
                    value = entry.eslabones.orEmpty(),
                    onValueChange = { value ->
                        viewModel.updateProblemChain(entry.label) { it.copy(eslabones = value) }
                    },
                    minLines = 3,
                    modifier = Modifier.weight(1f)
                )
                FormTextField(
                    label = "",
                    value = entry.problemasConducta.orEmpty(),
                    onValueChange = { value ->
                        viewModel.updateProblemChain(entry.label) { it.copy(problemasConducta = value) }
                    },
                    minLines = 3,
                    modifier = Modifier.weight(1f)
                )
                FormTextField(
                    label = "",
                    value = entry.consecuentes.orEmpty(),
                    onValueChange = { value ->
                        viewModel.updateProblemChain(entry.label) { it.copy(consecuentes = value) }
                    },
                    minLines = 3,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
    FormTextField(
        label = "Lista de metas asociadas a los problemas principales",
        value = state.problemGoals?.metas.orEmpty(),
        onValueChange = { viewModel.updateProblemGoals(it) },
        minLines = 3
    )
}

@Composable
private fun PsychometricsSection(state: SessionFormState, viewModel: SessionFormViewModel) {
    val data = state.psychometrics ?: defaultPsychometricsStub(state.session?.id)
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            FormTextField(
                label = "Cociente intelectual",
                value = data.coeficienteValor.orEmpty(),
                onValueChange = { value -> viewModel.updatePsychometrics { it.copy(coeficienteValor = value) } },
                modifier = Modifier.weight(1f),
                minLines = 2
            )
            FormTextField(
                label = "ClasificaciÃ³n",
                value = data.coeficienteClasificacion.orEmpty(),
                onValueChange = { value -> viewModel.updatePsychometrics { it.copy(coeficienteClasificacion = value) } },
                modifier = Modifier.weight(1f),
                minLines = 2
            )
        }
        FormTextField(
            label = "Temperamento",
            value = data.temperamento.orEmpty(),
            onValueChange = { value -> viewModel.updatePsychometrics { it.copy(temperamento = value) } },
            minLines = 2
        )
        FormTextField(
            label = "Personalidad y rasgos importantes",
            value = data.personalidad.orEmpty(),
            onValueChange = { value -> viewModel.updatePsychometrics { it.copy(personalidad = value) } },
            minLines = 2
        )
        FormTextField(
            label = "AtenciÃ³n y concentraciÃ³n",
            value = data.atencion.orEmpty(),
            onValueChange = { value -> viewModel.updatePsychometrics { it.copy(atencion = value) } },
            minLines = 2
        )
        FormTextField(
            label = "Problemas de conducta",
            value = data.problemasConducta.orEmpty(),
            onValueChange = { value -> viewModel.updatePsychometrics { it.copy(problemasConducta = value) } },
            minLines = 2
        )
        FormTextField(
            label = "DinÃ¡mica familiar",
            value = data.dinamicaFamiliar.orEmpty(),
            onValueChange = { value -> viewModel.updatePsychometrics { it.copy(dinamicaFamiliar = value) } },
            minLines = 2
        )
        FormTextField(
            label = "Otros de interÃ©s",
            value = data.otrosInteres.orEmpty(),
            onValueChange = { value -> viewModel.updatePsychometrics { it.copy(otrosInteres = value) } },
            minLines = 2
        )
    }
}

@Composable
private fun DysregulationSection(state: SessionFormState, viewModel: SessionFormViewModel) {
    val data = state.dysregulation ?: defaultDysregulationStub(state.session?.id)
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        FormTextField(
            label = "Emocional",
            value = data.emocional.orEmpty(),
            onValueChange = { value -> viewModel.updateDysregulation { it.copy(emocional = value) } },
            minLines = 2
        )
        FormTextField(
            label = "Conductual",
            value = data.conductual.orEmpty(),
            onValueChange = { value -> viewModel.updateDysregulation { it.copy(conductual = value) } },
            minLines = 2
        )
        FormTextField(
            label = "Interpersonal",
            value = data.interpersonal.orEmpty(),
            onValueChange = { value -> viewModel.updateDysregulation { it.copy(interpersonal = value) } },
            minLines = 2
        )
        FormTextField(
            label = "Del self - valores",
            value = data.selfValores.orEmpty(),
            onValueChange = { value -> viewModel.updateDysregulation { it.copy(selfValores = value) } },
            minLines = 2
        )
        FormTextField(
            label = "Cognitiva",
            value = data.cognitiva.orEmpty(),
            onValueChange = { value -> viewModel.updateDysregulation { it.copy(cognitiva = value) } },
            minLines = 2
        )
        FormTextField(
            label = "Resumen",
            value = data.resumen.orEmpty(),
            onValueChange = { value -> viewModel.updateDysregulation { it.copy(resumen = value) } },
            minLines = 2
        )
        FormCell(
            label = "AplicaciÃ³n de BsL-23 (siempre y cuando se sospeche de TLP)",
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color.White
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (data.bsl23Aplicado) "Aplicado" else "No aplicado",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = data.bsl23Aplicado,
                    onCheckedChange = { checked -> viewModel.updateDysregulation { it.copy(bsl23Aplicado = checked) } }
                )
            }
        }
    }
}

@Composable
private fun BiosocialSection(state: SessionFormState, viewModel: SessionFormViewModel) {
    val data = state.biosocial ?: defaultBiosocialStub(state.session?.id)
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        FormTextField(
            label = "Vulnerabilidad emocional",
            value = data.vulnerabilidadEmocional.orEmpty(),
            onValueChange = { value -> viewModel.updateBiosocial { it.copy(vulnerabilidadEmocional = value) } },
            minLines = 2
        )
        FormTextField(
            label = "Sensibilidad",
            value = data.sensibilidad.orEmpty(),
            onValueChange = { value -> viewModel.updateBiosocial { it.copy(sensibilidad = value) } },
            minLines = 2
        )
        FormTextField(
            label = "Intensidad",
            value = data.intensidad.orEmpty(),
            onValueChange = { value -> viewModel.updateBiosocial { it.copy(intensidad = value) } },
            minLines = 2
        )
        FormTextField(
            label = "Lento retorno a la calma",
            value = data.lentoRetornoCalma.orEmpty(),
            onValueChange = { value -> viewModel.updateBiosocial { it.copy(lentoRetornoCalma = value) } },
            minLines = 2
        )
        FormTextField(
            label = "InvalidaciÃ³n ambiental",
            value = data.invalidacionAmbiental.orEmpty(),
            onValueChange = { value -> viewModel.updateBiosocial { it.copy(invalidacionAmbiental = value) } },
            minLines = 2
        )
        FormTextField(
            label = "Criticar emociones",
            value = data.criticarEmociones.orEmpty(),
            onValueChange = { value -> viewModel.updateBiosocial { it.copy(criticarEmociones = value) } },
            minLines = 2
        )
        FormTextField(
            label = "Otros",
            value = data.otros.orEmpty(),
            onValueChange = { value -> viewModel.updateBiosocial { it.copy(otros = value) } },
            minLines = 2
        )
    }
}

@Composable
private fun TreatmentObjectivesSection(state: SessionFormState, viewModel: SessionFormViewModel) {
    val objectives = state.treatmentObjectives
    val grouped = SessionFormMetadata.treatmentFields.groupBy { it.stage }
    val stageEntries = grouped.entries.toList()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        stageEntries.forEachIndexed { index, (stage, definitions) ->
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.Black)
                        .background(Color(0xFF4A4A4A))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stageLabel(stage).uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
                definitions.forEach { definition ->
                    val value = objectives.firstOrNull { it.stage == stage && it.field == definition.field }?.value.orEmpty()
                    FormTextField(
                        label = definition.label,
                        value = value,
                        onValueChange = { text -> viewModel.updateTreatmentObjective(stage, definition.field, text) },
                        minLines = 2
                    )
                }
            }
            if (index != stageEntries.lastIndex) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ProblemAnalysisSection(state: SessionFormState, viewModel: SessionFormViewModel) {
    val analyses = state.problemAnalyses.sortedBy { it.problemNumber }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        analyses.forEach { analysis ->
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.Black)
                        .background(Color(0xFF4A4A4A))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Comportamiento problema ${analysis.problemNumber} (DFI)",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
                FormTextField(
                    label = "DescripciÃ³n",
                    value = analysis.comportamiento.orEmpty(),
                    onValueChange = { value ->
                        viewModel.updateProblemAnalysis(analysis.problemNumber) { it.copy(comportamiento = value) }
                    },
                    minLines = 2
                )
                FormTextField(
                    label = "AnÃ¡lisis de la soluciÃ³n",
                    value = analysis.analisisSolucion.orEmpty(),
                    onValueChange = { value ->
                        viewModel.updateProblemAnalysis(analysis.problemNumber) { it.copy(analisisSolucion = value) }
                    },
                    minLines = 2
                )
                FormTextField(
                    label = "Vulnerabilidad",
                    value = analysis.vulnerabilidad.orEmpty(),
                    onValueChange = { value ->
                        viewModel.updateProblemAnalysis(analysis.problemNumber) { it.copy(vulnerabilidad = value) }
                    },
                    minLines = 2
                )
                FormTextField(
                    label = "Evento precipitante externo",
                    value = analysis.eventoExterno.orEmpty(),
                    onValueChange = { value ->
                        viewModel.updateProblemAnalysis(analysis.problemNumber) { it.copy(eventoExterno = value) }
                    },
                    minLines = 2
                )
                FormTextField(
                    label = "Pensamientos",
                    value = analysis.pensamientos.orEmpty(),
                    onValueChange = { value ->
                        viewModel.updateProblemAnalysis(analysis.problemNumber) { it.copy(pensamientos = value) }
                    },
                    minLines = 2
                )
                FormTextField(
                    label = "Sensaciones",
                    value = analysis.sensaciones.orEmpty(),
                    onValueChange = { value ->
                        viewModel.updateProblemAnalysis(analysis.problemNumber) { it.copy(sensaciones = value) }
                    },
                    minLines = 2
                )
                FormTextField(
                    label = "Impulsos",
                    value = analysis.impulsos.orEmpty(),
                    onValueChange = { value ->
                        viewModel.updateProblemAnalysis(analysis.problemNumber) { it.copy(impulsos = value) }
                    },
                    minLines = 2
                )
                FormTextField(
                    label = "Emociones",
                    value = analysis.emociones.orEmpty(),
                    onValueChange = { value ->
                        viewModel.updateProblemAnalysis(analysis.problemNumber) { it.copy(emociones = value) }
                    },
                    minLines = 2
                )
                FormTextField(
                    label = "Consecuencias inmediatas reforzantes",
                    value = analysis.consecuenciasInmediatas.orEmpty(),
                    onValueChange = { value ->
                        viewModel.updateProblemAnalysis(analysis.problemNumber) { it.copy(consecuenciasInmediatas = value) }
                    },
                    minLines = 2
                )
                FormTextField(
                    label = "Consecuencias demoradas",
                    value = analysis.consecuenciasDemoradas.orEmpty(),
                    onValueChange = { value ->
                        viewModel.updateProblemAnalysis(analysis.problemNumber) { it.copy(consecuenciasDemoradas = value) }
                    },
                    minLines = 2
                )
                FormTextField(
                    label = "Resuma el plan de crisis",
                    value = analysis.planCrisis.orEmpty(),
                    onValueChange = { value ->
                        viewModel.updateProblemAnalysis(analysis.problemNumber) { it.copy(planCrisis = value) }
                    },
                    minLines = 2
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { viewModel.removeProblemAnalysis(analysis.problemNumber) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Eliminar problema")
                    }
                    Text(
                        text = "#${analysis.problemNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    FormTextField(
                        label = "SesiÃ³n",
                        value = note.titulo,
                        onValueChange = { value -> viewModel.updateEvolutionNote(note.id) { it.copy(titulo = value) } },
                        modifier = Modifier.weight(1f),
                        minLines = 1,
                        labelColor = Color(0xFFD32F2F)
                    )
                    FormTextField(
                        label = "Fecha",
                        value = note.notaFecha.orEmpty(),
                        onValueChange = { value -> viewModel.updateEvolutionNote(note.id) { it.copy(notaFecha = value) } },
                        modifier = Modifier.weight(1f),
                        minLines = 1
                    )
                    FormTextField(
                        label = "Comportamiento problema trabajado",
                        value = note.comportamientoTrabajado.orEmpty(),
                        onValueChange = { value -> viewModel.updateEvolutionNote(note.id) { it.copy(comportamientoTrabajado = value) } },
                        modifier = Modifier.weight(1.5f),
                        minLines = 1
                    )
                }
                FormTextField(
                    label = "Apuntes",
                    value = note.apuntes.orEmpty(),
                    onValueChange = { value -> viewModel.updateEvolutionNote(note.id) { it.copy(apuntes = value) } },
                    minLines = 3
                )
                FormTextField(
                    label = "Tareas",
                    value = note.tareas.orEmpty(),
                    onValueChange = { value -> viewModel.updateEvolutionNote(note.id) { it.copy(tareas = value) } },
                    minLines = 3
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { viewModel.removeEvolutionNote(note.id) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Eliminar registro")
                    }
                }
            }
        }
        Button(onClick = { viewModel.addEvolutionNote() }) {
            Text("Agregar sesiÃ³n")
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
            placeholder = { Text("Describe las tareas y aÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â±ade tags como [archivo.pdf]") },
            label = { Text("DescripciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n de tareas") },
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
@Composable
    modifier: Modifier = Modifier,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    backgroundColor: Color = Color.White,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .border(1.dp, Color.Black)
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = labelColor
            )
        }
        content()
    }
}
    label: String,
    modifier: Modifier = Modifier,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .border(1.dp, Color.Black)
            .background(Color.White)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = labelColor
        )
        content()
    }
}

@Composable
private fun FormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    FormCell(label = label, modifier = modifier, labelColor = labelColor) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyMedium,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = keyboardOptions,
            minLines = minLines,
            maxLines = if (minLines == 1) 1 else Int.MAX_VALUE,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = (minLines * 24).dp),
            decorationBox = { inner ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (value.isEmpty() && !placeholder.isNullOrBlank()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    inner()
                }
            }
        )
    }
}

@Composable
private fun FormReadOnlyField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    emptyPlaceholder: String = "-"
) {
    val display = value.ifBlank { emptyPlaceholder }
    val displayColor = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    FormCell(label = label, modifier = modifier, labelColor = labelColor) {
        Text(
            text = display,
            style = MaterialTheme.typography.bodyMedium,
            color = displayColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormDateField(
    label: String,
    value: LocalDate?,
    onValueChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    labelColor: Color = MaterialTheme.colorScheme.onSurface
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val formatted = value?.let { formatDate(it) }.orEmpty()

    FormCell(label = label, modifier = modifier, labelColor = labelColor) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (formatted.isEmpty()) "Seleccionar fecha" else formatted,
                style = MaterialTheme.typography.bodyMedium,
                color = if (formatted.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Outlined.DateRange,
                contentDescription = "Seleccionar fecha",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    if (showDialog) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = value?.toEpochMillis())
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = datePickerState.selectedDateMillis?.let { millis ->
                            Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).date
                        }
                        onValueChange(selected)
                        showDialog = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }
}

@Composable
private fun RowScope.TableHeaderCell(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF4A4A4A)
) {
    Box(
        modifier = modifier
            .border(1.dp, Color.Black)
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )
    }
}
    label: String,

@Composable
private fun RowScope.TableHeaderCell(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF4A4A4A)
) {
    Box(
        modifier = modifier
            .border(1.dp, Color.Black)
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )
    }
}
    value: LocalDate?,
    onValueChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    labelColor: Color = MaterialTheme.colorScheme.onSurface
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val formatted = value?.let { formatDate(it) }.orEmpty()

    FormCell(label = label, modifier = modifier, labelColor = labelColor) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (formatted.isEmpty()) "Seleccionar fecha" else formatted,
                style = MaterialTheme.typography.bodyMedium,
                color = if (formatted.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Outlined.DateRange,
                contentDescription = "Seleccionar fecha",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    if (showDialog) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = value?.toEpochMillis())
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = datePickerState.selectedDateMillis?.let { millis ->
                            Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).date
                        }
                        onValueChange(selected)
                        showDialog = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }
private fun ControlledLargeTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    minHeight: Dp = 120.dp
) {
    val estimatedLines = max((minHeight.value / 24f).roundToInt(), 3)
    FormTextField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        minLines = estimatedLines,
        placeholder = placeholder
    )
}
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
        val ageText = birthDate?.let { "${calculateAge(it)} aÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â±os" } ?: "ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â"
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
