package com.clinica.desktop

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.clinica.app.AppInitializer
import com.clinica.data.DesktopDatabaseFactory
import java.nio.file.Path
import androidx.compose.foundation.layout.Spacer

fun main() = application {
    val appDir = Path.of(System.getProperty("user.home"), ".psych-notes")
    val storageRoot = appDir.resolve("storage")
    AppInitializer.init(DesktopDatabaseFactory(appDir))

    Window(onCloseRequest = ::exitApplication, title = "Psych Notes") {
        MaterialTheme {
            Surface {
                PsychNotesScreen(storageRoot, window)
            }
        }
    }
}

@Composable
fun PsychNotesScreen(
    storageRoot: Path = Path.of(System.getProperty("user.home"), ".psych-notes", "storage"),
    composeWindow: ComposeWindow? = null
) {
    Column(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text("Notas Psicoterapéuticas", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.padding(8.dp))
        Text("Inicia creando un paciente y una ficha. La interfaz final replicará la ficha original pero optimizada para digital.")
        Spacer(modifier = Modifier.padding(8.dp))
        SessionFormScreen(storageRoot = storageRoot, composeWindow = composeWindow)
    }
}

@Preview
@Composable
fun PreviewPsychNotes() {
    MaterialTheme {
        Surface {
            PsychNotesScreen()
        }
    }
}
