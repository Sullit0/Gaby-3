package com.clinica.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clinica.desktop.calculateAge
import com.clinica.app.form.SessionFormViewModel
import com.clinica.domain.model.Patient
import com.clinica.domain.model.Session
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
import java.awt.Desktop

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
                            PatientTableRow(
                                patient = patient,
                                lastSession = getLastSessionDate(patient.id) ?: "Sin sesiones",
                                onSelect = { onPatientSelect(patient.id) },
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
            IconButton(
                onClick = {
                    scope.launch {
                        try {
                            // Obtener el estado completo del formulario para este paciente
                            val formState = viewModel.createSessionStateForPatient(patient.id)
                            if (formState != null) {
                                // Generar el PDF
                                val downloadsFolder = System.getProperty("user.home") + "/Downloads"
                                val fileName = "Ficha_${patient.displayName.replace(" ", "_")}_${java.time.LocalDate.now()}.txt"
                                val outputPath = java.nio.file.Paths.get(downloadsFolder, fileName)
                                
                                val success = PDFGenerator.generatePDF(formState, outputPath)
                                if (success) {
                                    // Abrir el archivo generado
                                    if (Desktop.isDesktopSupported()) {
                                        Desktop.getDesktop().open(outputPath.toFile())
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Manejar error de impresión
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
        }
    }
}
