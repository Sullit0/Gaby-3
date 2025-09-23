package com.clinica.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clinica.ui.factory.*

@Composable
fun DynamicMainNavigation(
    onModuleSelected: (EntityType) -> Unit,
    currentModule: EntityType?
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header dinámico
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentModule?.let { ViewFactory.getTitle(it) } ?: "Psych Notes",
                    style = MaterialTheme.typography.headlineSmall
                )

                // Menú desplegable de módulos
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    availableModules
                        .filter { it.enabled }
                        .forEach { module ->
                            DropdownMenuItem(
                                text = { Text(module.title) },
                                onClick = {
                                    onModuleSelected(module.entityType)
                                    showMenu = false
                                }
                            )
                        }
                }

                Button(onClick = { showMenu = true }) {
                    Text("Módulos")
                }
            }
        }

        // Contenido dinámico
        currentModule?.let { module ->
            ViewFactory.getListScreen(
                entityType = module,
                onItemSelected = { /* Navegar a formulario */ },
                onAddNew = { /* Navegar a formulario nuevo */ }
            )
        }
    }
}

@Composable
fun ModuleSelector(
    selectedModule: EntityType,
    onModuleSelected: (EntityType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        availableModules
            .filter { it.enabled }
            .forEach { module ->
                FilterChip(
                    selected = selectedModule == module.entityType,
                    onClick = { onModuleSelected(module.entityType) },
                    label = { Text(module.title) }
                )
            }
    }
}