package com.clinica.desktop

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.clinica.app.AppInitializer
import com.clinica.data.DesktopDatabaseFactory
import java.nio.file.Path
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import java.util.prefs.Preferences

private val prefs = Preferences.userNodeForPackage(object {}::class.java.enclosingClass)

fun loadThemePreference(): Boolean {
    return prefs.getBoolean("dark_theme", false)
}

fun saveThemePreference(isDark: Boolean) {
    prefs.putBoolean("dark_theme", isDark)
}

fun main() = application {
    val appDir = Path.of(System.getProperty("user.home"), ".psych-notes")
    val storageRoot = appDir.resolve("storage")
    AppInitializer.init(DesktopDatabaseFactory(appDir))

    Window(onCloseRequest = ::exitApplication, title = "Psych Notes") {
        var isDarkTheme by remember { mutableStateOf(loadThemePreference()) }
        var showPatientsList by remember { mutableStateOf(false) }
        var selectedPatientId by remember { mutableStateOf<String?>(null) }

        MaterialTheme(
            colorScheme = if (isDarkTheme) {
                // Modo oscuro profesional médico
                androidx.compose.material3.darkColorScheme(
                    primary = androidx.compose.ui.graphics.Color(0xFF90CAF9),      // Azul más brillante
                    primaryContainer = androidx.compose.ui.graphics.Color(0xFF1E3A5F), // Azul oscuro para contenedores
                    secondary = androidx.compose.ui.graphics.Color(0xFF80DEEA),     // Cyan más brillante
                    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF1E4B5F), // Cyan oscuro
                    tertiary = androidx.compose.ui.graphics.Color(0xFFCE93D8),      // Púrpura más brillante
                    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF4A2C4A), // Púrpura oscuro
                    surface = androidx.compose.ui.graphics.Color(0xFF1E1E1E),       // Fondo un poco más claro
                    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF2D2D30), // Variantes de superficie
                    background = androidx.compose.ui.graphics.Color(0xFF121212),     // Fondo principal
                    onPrimary = androidx.compose.ui.graphics.Color(0xFF000000),      // Texto en primario
                    onSecondary = androidx.compose.ui.graphics.Color(0xFF000000),    // Texto en secundario
                    onTertiary = androidx.compose.ui.graphics.Color(0xFF000000),     // Texto en terciario
                    onSurface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),     // Texto principal blanco puro
                    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFE0E0E0), // Texto en variantes más brillante
                    outline = androidx.compose.ui.graphics.Color(0xFF9E9E9E),        // Bordes más visibles
                    outlineVariant = androidx.compose.ui.graphics.Color(0xFF757575),  // Bordes variantes
                    error = androidx.compose.ui.graphics.Color(0xFFCF6679),          // Rojo error suave
                    onError = androidx.compose.ui.graphics.Color(0xFF000000),        // Texto en error
                    inverseSurface = androidx.compose.ui.graphics.Color(0xFFE0E0E0),  // Superficie inversa
                    inverseOnSurface = androidx.compose.ui.graphics.Color(0xFF121212)   // Texto en superficie inversa
                )
            } else {
                // Modo claro mejorado
                androidx.compose.material3.lightColorScheme(
                    primary = androidx.compose.ui.graphics.Color(0xFF1976D2),      // Azul profesional
                    primaryContainer = androidx.compose.ui.graphics.Color(0xFFE3F2FD), // Azul muy claro
                    secondary = androidx.compose.ui.graphics.Color(0xFF0288D1),     // Cyan
                    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFE0F7FA), // Cyan muy claro
                    tertiary = androidx.compose.ui.graphics.Color(0xFF7B1FA2),      // Púrpura
                    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFF3E5F5), // Púrpura muy claro
                    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),       // Blanco puro
                    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF5F5F5), // Gris muy claro
                    background = androidx.compose.ui.graphics.Color(0xFFFFFFFF),     // Blanco
                    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),      // Blanco en primario
                    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),    // Blanco en secundario
                    onTertiary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),     // Blanco en terciario
                    onSurface = androidx.compose.ui.graphics.Color(0xFF1C1B1F),     // Negro suave
                    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF49454F), // Gris medio
                    outline = androidx.compose.ui.graphics.Color(0xFF79747E),        // Bordes grises
                    outlineVariant = androidx.compose.ui.graphics.Color(0xFFC4C7C5),  // Bordes claros
                    error = androidx.compose.ui.graphics.Color(0xFFBA1A1A),          // Rojo error
                    onError = androidx.compose.ui.graphics.Color(0xFFFFFFFF)        // Blanco en error
                )
            }
        ) {
            Surface(
                color = MaterialTheme.colorScheme.background
            ) {
                PsychNotesScreen(
                    storageRoot = storageRoot,
                    composeWindow = window,
                    isDarkTheme = isDarkTheme,
                    showPatientsList = showPatientsList,
                    onThemeToggle = {
                        isDarkTheme = !isDarkTheme
                        saveThemePreference(isDarkTheme)
                    },
                    onToggleView = {
                        showPatientsList = !showPatientsList
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PsychNotesScreen(
    storageRoot: Path = Path.of(System.getProperty("user.home"), ".psych-notes", "storage"),
    composeWindow: ComposeWindow? = null,
    isDarkTheme: Boolean = false,
    showPatientsList: Boolean = false,
    onThemeToggle: () -> Unit = {},
    onToggleView: () -> Unit = {},
   ) {
    var selectedPatientId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Top App Bar con toggle de vista, imprimir y tema
        TopAppBar(
            title = {
                Text(
                    text = "Psych Notes",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            actions = {
                IconButton(
                    onClick = onToggleView,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = if (showPatientsList) Icons.Filled.Description else Icons.Filled.Person,
                        contentDescription = if (showPatientsList) "Ver formulario" else "Ver lista de pacientes",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onThemeToggle,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = if (isDarkTheme) "Cambiar a tema claro" else "Cambiar a tema oscuro",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        // Contenido principal según la vista seleccionada
        if (showPatientsList) {
            PatientsTableScreen(
                onPatientSelect = { patientId ->
                    selectedPatientId = patientId
                    onToggleView() // Cambiar a vista de formulario
                },
                onNewPatient = {
                    selectedPatientId = null // Limpiar selección para nuevo paciente
                    onToggleView() // Cambiar a vista de formulario para crear nuevo paciente
                }
            )
        } else {
            SessionFormScreen(
                storageRoot = storageRoot,
                composeWindow = composeWindow,
                selectedPatientId = selectedPatientId
            )
        }
    }
}

@Preview
@Composable
fun PreviewPsychNotes() {
    var isDarkTheme by remember { mutableStateOf(false) }
    var showPatientsList by remember { mutableStateOf(false) }

    MaterialTheme(
        colorScheme = if (isDarkTheme) {
            // Modo oscuro profesional médico
            androidx.compose.material3.darkColorScheme(
                    primary = androidx.compose.ui.graphics.Color(0xFF90CAF9),      // Azul más brillante
                    primaryContainer = androidx.compose.ui.graphics.Color(0xFF1E3A5F), // Azul oscuro para contenedores
                    secondary = androidx.compose.ui.graphics.Color(0xFF80DEEA),     // Cyan más brillante
                    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF1E4B5F), // Cyan oscuro
                    tertiary = androidx.compose.ui.graphics.Color(0xFFCE93D8),      // Púrpura más brillante
                    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF4A2C4A), // Púrpura oscuro
                    surface = androidx.compose.ui.graphics.Color(0xFF1E1E1E),       // Fondo un poco más claro
                    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF2D2D30), // Variantes de superficie
                    background = androidx.compose.ui.graphics.Color(0xFF121212),     // Fondo principal
                    onPrimary = androidx.compose.ui.graphics.Color(0xFF000000),      // Texto en primario
                    onSecondary = androidx.compose.ui.graphics.Color(0xFF000000),    // Texto en secundario
                    onTertiary = androidx.compose.ui.graphics.Color(0xFF000000),     // Texto en terciario
                    onSurface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),     // Texto principal blanco puro
                    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFE0E0E0), // Texto en variantes más brillante
                    outline = androidx.compose.ui.graphics.Color(0xFF9E9E9E),        // Bordes más visibles
                    outlineVariant = androidx.compose.ui.graphics.Color(0xFF757575),  // Bordes variantes
                    error = androidx.compose.ui.graphics.Color(0xFFCF6679),          // Rojo error suave
                    onError = androidx.compose.ui.graphics.Color(0xFF000000),        // Texto en error
                    inverseSurface = androidx.compose.ui.graphics.Color(0xFFE0E0E0),  // Superficie inversa
                    inverseOnSurface = androidx.compose.ui.graphics.Color(0xFF121212)   // Texto en superficie inversa
                )
        } else {
            // Modo claro mejorado
            androidx.compose.material3.lightColorScheme(
                primary = androidx.compose.ui.graphics.Color(0xFF1976D2),      // Azul profesional
                primaryContainer = androidx.compose.ui.graphics.Color(0xFFE3F2FD), // Azul muy claro
                secondary = androidx.compose.ui.graphics.Color(0xFF0288D1),     // Cyan
                secondaryContainer = androidx.compose.ui.graphics.Color(0xFFE0F7FA), // Cyan muy claro
                tertiary = androidx.compose.ui.graphics.Color(0xFF7B1FA2),      // Púrpura
                tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFF3E5F5), // Púrpura muy claro
                surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),       // Blanco puro
                surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF5F5F5), // Gris muy claro
                background = androidx.compose.ui.graphics.Color(0xFFFFFFFF),     // Blanco
                onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),      // Blanco en primario
                onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),    // Blanco en secundario
                onTertiary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),     // Blanco en terciario
                onSurface = androidx.compose.ui.graphics.Color(0xFF1C1B1F),     // Negro suave
                onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF49454F), // Gris medio
                outline = androidx.compose.ui.graphics.Color(0xFF79747E),        // Bordes grises
                outlineVariant = androidx.compose.ui.graphics.Color(0xFFC4C7C5),  // Bordes claros
                error = androidx.compose.ui.graphics.Color(0xFFBA1A1A),          // Rojo error
                onError = androidx.compose.ui.graphics.Color(0xFFFFFFFF)        // Blanco en error
            )
        }
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            PsychNotesScreen(
                isDarkTheme = isDarkTheme,
                showPatientsList = showPatientsList,
                onThemeToggle = { isDarkTheme = !isDarkTheme },
                onToggleView = { showPatientsList = !showPatientsList },
              )
        }
    }
}
