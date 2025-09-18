package com.clinica.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clinica.app.form.SessionFormViewModel
import com.clinica.app.form.SessionFormState
import com.clinica.domain.model.Attachment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.GlobalContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import com.benasher44.uuid.uuid4
import kotlin.io.DEFAULT_BUFFER_SIZE

@Composable
fun SessionFormScreen(modifier: Modifier = Modifier, storageRoot: Path) {
    val koin = GlobalContext.get()
    val viewModel = remember {
        SessionFormViewModel(
            patientRepository = koin.get(),
            sessionRepository = koin.get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        )
    }
    DisposableEffect(Unit) {
        viewModel.loadDefaultPatient()
        onDispose { viewModel.dispose() }
    }

    val state by viewModel.state.collectAsState()

    val scrollState = rememberScrollState()
    if (state.isLoading) {
        Column(
            modifier = modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.padding(8.dp))
            Text("Cargando ficha...")
        }
        return
    }

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(bottom = 64.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle("Identificación del paciente")
        IdentificationSection(state, viewModel)

        SectionTitle("Datos familiares de interés")
        LargeTextField(label = "Descripción")

        SectionTitle("Análisis en cadena de los problemas principales")
        ProblemChainSection()

        SectionTitle("Datos psicométricos (de corresponder)")
        PsychometricSection()

        SectionTitle("Áreas de desregulación")
        DysregulationSection()

        SectionTitle("Apuntes de evolución psicoterapéutica")
        EvolutionSection()

        SectionTitle("Tareas / Adjuntos de la sesión")
        TasksSection(state, viewModel, storageRoot)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IdentificationSection(
    state: SessionFormState,
    viewModel: SessionFormViewModel
) {
    val patient = state.patient
    val session = state.session

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = patient?.displayName.orEmpty(),
                onValueChange = { viewModel.updatePatientName(it) },
                label = { Text("Apellidos y nombres") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = session?.firstAttentionDate?.toString().orEmpty(),
                    onValueChange = { viewModel.updateFirstAttentionRaw(it) },
                    label = { Text("Fecha de primera atención") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = patient?.gender.orEmpty(),
                    onValueChange = { viewModel.updateGender(it) },
                    label = { Text("Género") },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = patient?.birthDate?.toString().orEmpty(),
                    onValueChange = { viewModel.updateBirthDateRaw(it) },
                    label = { Text("Fecha de nacimiento") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = patient?.birthDate?.let { calculateAge(it).toString() }.orEmpty(),
                    onValueChange = { },
                    label = { Text("Edad") },
                    enabled = false,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
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
}

@Composable
private fun LargeTextField(
    label: String,
    valueState: MutableState<String>? = null,
) {
    val state = valueState ?: remember { mutableStateOf("") }
    OutlinedTextField(
        value = state.value,
        onValueChange = { state.value = it },
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        maxLines = 8
    )
}

@Composable
private fun ControlledLargeTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        maxLines = 8
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProblemChainSection() {
    val rows = remember {
        mutableStateOf(
            listOf(
                ProblemChainRowState("P1"),
                ProblemChainRowState("P2"),
                ProblemChainRowState("P3"),
                ProblemChainRowState("P4")
            )
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.value.forEach { rowState ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(rowState.label, style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = rowState.vulnerabilidades.value,
                        onValueChange = { rowState.vulnerabilidades.value = it },
                        label = { Text("Vulnerabilidades") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = rowState.evento.value,
                        onValueChange = { rowState.evento.value = it },
                        label = { Text("Evento desencadenante") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = rowState.eslabones.value,
                        onValueChange = { rowState.eslabones.value = it },
                        label = { Text("Eslabones") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = rowState.problemas.value,
                        onValueChange = { rowState.problemas.value = it },
                        label = { Text("Problemas de conducta o crisis") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = rowState.consecuentes.value,
                        onValueChange = { rowState.consecuentes.value = it },
                        label = { Text("Consecuentes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        LargeTextField(label = "Lista de metas asociadas")
    }
}

private data class ProblemChainRowState(
    val label: String,
    val vulnerabilidades: MutableState<String> = mutableStateOf(""),
    val evento: MutableState<String> = mutableStateOf(""),
    val eslabones: MutableState<String> = mutableStateOf(""),
    val problemas: MutableState<String> = mutableStateOf(""),
    val consecuentes: MutableState<String> = mutableStateOf("")
)

@Composable
private fun PsychometricSection() {
    LargeTextField(label = "Cociente intelectual (dato cuantitativo y clasificación)")
    LargeTextField(label = "Temperamento")
    LargeTextField(label = "Personalidad y rasgos importantes")
    LargeTextField(label = "Atención y concentración")
    LargeTextField(label = "Problemas de conducta")
    LargeTextField(label = "Dinámica familiar")
    LargeTextField(label = "Otros de interés")
}

@Composable
private fun DysregulationSection() {
    LargeTextField(label = "Emocional")
    LargeTextField(label = "Conductual")
    LargeTextField(label = "Interpersonal")
    LargeTextField(label = "Del self - valores")
    LargeTextField(label = "Cognitiva")
    LargeTextField(label = "Resumen")
}

@Composable
private fun EvolutionSection() {
    LargeTextField(label = "Sesión I - Apuntes")
    LargeTextField(label = "Tareas previas")
}

@Composable
private fun TasksSection(state: SessionFormState, viewModel: SessionFormViewModel, storageRoot: Path) {
    ControlledLargeTextField(
        label = "Descripción de tareas",
        value = state.tasks?.descripcion.orEmpty(),
        onValueChange = { viewModel.updateTasks(it) }
    )
    Spacer(modifier = Modifier.padding(vertical = 8.dp))
    AttachmentControls(state, viewModel, storageRoot)
}

@Composable
private fun AttachmentControls(state: SessionFormState, viewModel: SessionFormViewModel, storageRoot: Path) {
    val patientId = state.patient?.id
    val sessionId = state.session?.id
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = {
            if (patientId == null || sessionId == null) return@Button
            val files = chooseFiles() ?: return@Button
            scope.launch(Dispatchers.IO) {
                val created = processAttachments(files, storageRoot, patientId, sessionId)
                created.forEach { attachment ->
                    viewModel.registerAttachment(attachment)
                }
            }
        }) {
            Text("Agregar archivos")
        }

        if (state.attachments.isEmpty()) {
            Text("Aún no hay archivos adjuntos", style = MaterialTheme.typography.bodyMedium)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.attachments.forEach { attachment ->
                    AttachmentRow(
                        attachment = attachment,
                        storageRoot = storageRoot,
                        patientId = patientId,
                        onOpen = { openAttachment(it) },
                        onRemove = { path ->
                            scope.launch(Dispatchers.IO) {
                                Files.deleteIfExists(path)
                                viewModel.removeAttachment(attachment.id)
                            }
                        }
                    )
                }
            }
        }

        DropZonePlaceholder()
    }
}

@Composable
private fun DropZonePlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE0E0E0))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Arrastra y suelta archivos aquí (próximamente)")
        Text("PDF, DOCX, JPG, PNG", style = MaterialTheme.typography.bodySmall)
    }
}

private fun calculateAge(birthDate: LocalDate): Int {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    var age = today.year - birthDate.year
    val birthdayThisYear = LocalDate(today.year, birthDate.monthNumber, birthDate.dayOfMonth)
    if (today < birthdayThisYear) {
        age -= 1
    }
    return age
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

@Composable
private fun AttachmentRow(
    attachment: Attachment,
    storageRoot: Path,
    patientId: String?,
    onOpen: (Path) -> Unit,
    onRemove: (Path) -> Unit
) {
    val path = patientId?.let { storageRoot.resolve(it).resolve(attachment.sessionId).resolve(attachment.storedName) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF0F0F0))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(attachment.displayName, style = MaterialTheme.typography.bodyLarge)
            val sizeLabel = attachment.sizeBytes?.let { humanReadableSize(it) } ?: "Tamaño desconocido"
            Text(sizeLabel, style = MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val canOpen = path?.let { Files.exists(it) } == true
            Button(
                onClick = { path?.let { if (Files.exists(it)) onOpen(it) } },
                enabled = canOpen
            ) {
                Text("Abrir")
            }
            Button(
                onClick = { path?.let { onRemove(it) } },
                enabled = path != null
            ) {
                Text("Eliminar")
            }
        }
    }
}

private fun humanReadableSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

private fun openAttachment(path: Path) {
    if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().open(path.toFile())
    }
}
