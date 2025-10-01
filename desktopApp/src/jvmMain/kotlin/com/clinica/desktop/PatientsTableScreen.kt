package com.clinica.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.shape.RoundedCornerShape
import com.clinica.desktop.calculateAge
import com.clinica.app.form.SessionFormViewModel
import com.clinica.domain.model.Patient
import com.clinica.domain.model.Session
import com.clinica.domain.model.Attachment
import kotlinx.datetime.LocalDate
import java.nio.file.Files
import java.awt.Desktop
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.Instant
import kotlinx.datetime.periodUntil
import java.awt.print.PrinterJob
import java.awt.print.PrinterException
import java.awt.print.Printable
import java.awt.print.PageFormat
import java.awt.print.Paper
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Font
import javax.imageio.ImageIO
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientsTableScreen(
    onPatientSelect: (String) -> Unit,
    onNewPatient: () -> Unit,
    modifier: Modifier = Modifier
) {
    val koin = GlobalContext.get()
    val viewModel = remember {
        SessionFormViewModel(
            patientRepository = koin.get(),
            sessionRepository = koin.get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        )
    }

    var patients by remember { mutableStateOf<List<Patient>>(emptyList()) }
    var sessions by remember { mutableStateOf<List<Session>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var expandedPatientId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                patients = viewModel.patientRepository.getAllPatients()
                sessions = viewModel.sessionRepository.getAllSessions()
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
            }
        }
    }

    val filteredPatients = patients.filter {
        it.displayName.contains(searchQuery, ignoreCase = true) ||
        (it.dni?.contains(searchQuery, ignoreCase = true) ?: false)
    }

    // Obtener última sesión para cada paciente
    fun getLastSessionDate(patientId: String): String? {
        return sessions
            .filter { it.patientId == patientId }
            .maxByOrNull { it.createdAt }
            ?.createdAt?.let { instant ->
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val sessionDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
                val daysDiff = sessionDate.periodUntil(today).days

                when {
                    daysDiff == 0 -> "Hoy"
                    daysDiff == 1 -> "Ayer"
                    daysDiff < 7 -> "Hace $daysDiff días"
                    daysDiff < 30 -> "Hace ${daysDiff/7} semanas"
                    else -> sessionDate.toString()
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Gestión de Pacientes",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${filteredPatients.size} paciente${if (filteredPatients.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onNewPatient,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Nuevo Paciente")
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Buscar por nombre o DNI...") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            }
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (filteredPatients.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isEmpty()) "No hay pacientes registrados" else "No se encontraron pacientes",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Header de la tabla
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Paciente",
                            modifier = Modifier.weight(2f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "DNI",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Edad",
                            modifier = Modifier.weight(0.8f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Última Sesión",
                            modifier = Modifier.weight(1.2f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Contacto",
                            modifier = Modifier.weight(1.5f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Acciones",
                            modifier = Modifier.weight(1.5f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    HorizontalDivider()

                    // Filas de la tabla
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filteredPatients) { patient ->
                            val isExpanded = expandedPatientId == patient.id
                            
                            Column {
                                PatientTableRow(
                                    patient = patient,
                                    lastSession = getLastSessionDate(patient.id) ?: "Sin sesiones",
                                    isExpanded = isExpanded,
                                    onSelect = { 
                                        expandedPatientId = if (isExpanded) null else patient.id
                                    },
                                    onDelete = {
                                        scope.launch {
                                            try {
                                                viewModel.patientRepository.deletePatient(patient.id)
                                                patients = patients.filter { it.id != patient.id }
                                            } catch (e: Exception) {
                                                // Handle error
                                            }
                                        }
                                    },
                                    scope = scope,
                                    viewModel = viewModel
                                )
                                
                                // Panel expandible con detalles del paciente
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    PatientDetailsPanel(
                                        patient = patient,
                                        viewModel = viewModel,
                                        scope = scope
                                    )
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientTableRow(
    patient: Patient,
    lastSession: String,
    isExpanded: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    scope: CoroutineScope,
    viewModel: SessionFormViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Columna: Paciente
        Column(
            modifier = Modifier.weight(2f)
        ) {
            Text(
                text = patient.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            patient.gender?.takeIf { it.isNotEmpty() }?.let { gender ->
                Text(
                    text = gender,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Columna: DNI
        Text(
            text = patient.dni?.ifEmpty { "—" } ?: "—",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        // Columna: Edad
        Text(
            text = patient.birthDate?.let { "${calculateAge(it)} años" } ?: "—",
            modifier = Modifier.weight(0.8f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        // Columna: Última Sesión
        Text(
            text = lastSession,
            modifier = Modifier.weight(1.2f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Columna: Contacto
        Column(
            modifier = Modifier.weight(1.5f)
        ) {
            patient.phone?.takeIf { it.isNotEmpty() }?.let { phone ->
                Text(
                    text = phone,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            patient.address?.takeIf { it.isNotEmpty() }?.let { address ->
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        // Columna: Acciones
        Row(
            modifier = Modifier.weight(1.5f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onSelect,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Editar paciente",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = {
                    scope.launch {
                        try {
                            // Cargar los datos del paciente para imprimir
                            viewModel.loadPatientById(patient.id)
                            val templatePath = WordDocumentGenerator.ensureBundledTemplate()
                            
                            if (templatePath != null) {
                                println("Imprimiendo ficha de paciente: ${patient.displayName}")
                                val success = WordDocumentGenerator.fillTemplateAndPrintDirect(viewModel.state.value, templatePath)
                                if (success) {
                                    println("✅ Ficha enviada a impresión correctamente")
                                } else {
                                    println("❌ Error al imprimir ficha")
                                }
                            } else {
                                println("❌ No se pudo cargar plantilla o datos del paciente")
                            }
                        } catch (e: Exception) {
                            println("❌ Error al imprimir ficha: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Print,
                    contentDescription = "Imprimir ficha",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar paciente",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PatientDetailsPanel(
    patient: Patient,
    viewModel: SessionFormViewModel,
    scope: CoroutineScope
) {
    var attachments by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    var isLoadingAttachments by remember { mutableStateOf(true) }
    val storageRoot = Paths.get(System.getProperty("user.home"), "clinica_data")

    LaunchedEffect(patient.id) {
        scope.launch(Dispatchers.IO) {
            try {
                // Obtener todas las sesiones del paciente filtrando en memoria
                val patientSessions = viewModel.sessionRepository
                    .getAllSessions()
                    .filter { it.patientId == patient.id }

                // Unificar todos los adjuntos asociados a las sesiones del paciente
                val allAttachments = patientSessions.flatMap { session ->
                    viewModel.sessionRepository.getAttachments(session.id)
                }

                attachments = allAttachments
                isLoadingAttachments = false
            } catch (e: Exception) {
                println("Error al cargar adjuntos: ${e.message}")
                isLoadingAttachments = false
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header del panel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detalles del Paciente",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
            )

            // Información básica del paciente
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Información Personal",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoItem("DNI", patient.dni ?: "No especificado")
                    InfoItem("Género", patient.gender ?: "No especificado")
                    InfoItem("Edad", patient.birthDate?.let { "${calculateAge(it)} años" } ?: "No especificado")
                }
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Contacto",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoItem("Teléfono", patient.phone ?: "No especificado")
                    InfoItem("Dirección", patient.address ?: "No especificado")
                }
            }

            // Sección de adjuntos
            if (isLoadingAttachments) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            } else if (attachments.isNotEmpty()) {
                Column {
                    Text(
                        text = "Archivos Adjuntos (${attachments.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    attachments.forEach { attachment ->
                        AttachmentItem(
                            attachment = attachment,
                            storageRoot = storageRoot,
                            patientId = patient.id
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            } else {
                Text(
                    text = "No hay archivos adjuntos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun AttachmentItem(
    attachment: Attachment,
    storageRoot: Path,
    patientId: String
) {
    var isHovered by remember { mutableStateOf(false) }
    val readableSize = attachment.sizeBytes?.let { formatFileSize(it) } ?: "Tamaño desconocido"
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val path = storageRoot.resolve(patientId)
                    .resolve(attachment.sessionId)
                    .resolve(attachment.storedName)
                if (Files.exists(path)) {
                    try {
                        Desktop.getDesktop().open(path.toFile())
                    } catch (e: Exception) {
                        println("Error al abrir archivo: ${e.message}")
                    }
                }
            }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .background(
                if (isHovered) 
                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f) 
                else 
                    Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.AttachFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = attachment.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1
                )
                Text(
                    text = "$readableSize • ${attachment.mimeType ?: "Tipo desconocido"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
        Icon(
            Icons.Outlined.OpenInNew,
            contentDescription = "Abrir archivo",
            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = if (isHovered) 1f else 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

// Función para obtener la carpeta de descargas de forma multiplataforma
private fun getDownloadsFolder(): String {
    val osName = System.getProperty("os.name").lowercase()
    return when {
        osName.contains("win") -> {
            // Windows
            val userHome = System.getProperty("user.home")
            "$userHome\\Downloads"
        }
        osName.contains("mac") -> {
            // macOS
            val userHome = System.getProperty("user.home")
            "$userHome/Downloads"
        }
        else -> {
            // Linux y otros sistemas Unix-like
            val userHome = System.getProperty("user.home")
            "$userHome/Downloads"
        }
    }
}

// Función para abrir archivos adjuntos (como en FormScreen)
private fun openAttachment(path: java.nio.file.Path) {
    if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().open(path.toFile())
    }
}
