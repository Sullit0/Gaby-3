package com.clinica.ui.forms

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.benasher44.uuid.Uuid
import com.clinica.ui.components.*

// Data class genérica para formularios
data class FormField<T>(
    val value: T,
    val label: String,
    val onValueChange: (T) -> Unit,
    val validator: (T) -> Boolean = { true }
)

@Composable
fun <T> GenericEntityForm(
    entity: T?,
    fields: List<FormField<*>>,
    onSave: (T) -> Unit,
    onCancel: () -> Unit,
    entityToString: (T) -> String,
    isValid: (T) -> Boolean
) {
    var currentEntity by remember { mutableStateOf(entity) }

    GenericFormCard(
        title = if (entity == null) "Nuevo Registro" else "Editar: ${entityToString(entity)}"
    ) {
        // Campos dinámicos
        fields.forEach { field ->
            when (field.value) {
                is String -> {
                    val stringField = field as FormField<String>
                    GenericTextField(
                        label = stringField.label,
                        value = stringField.value,
                        onValueChange = stringField.onValueChange,
                        maxLines = if (stringField.label.contains("observaciones", ignoreCase = true)) 3 else 1
                    )
                }
                is Int -> {
                    val intField = field as FormField<Int>
                    GenericTextField(
                        label = intField.label,
                        value = intField.value.toString(),
                        onValueChange = { newValue ->
                            newValue.toIntOrNull()?.let { intField.onValueChange(it) }
                        }
                    )
                }
                // Agregar más tipos según necesidad
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        GenericButtonRow(
            onSave = { /* Lógica de guardado */ },
            onCancel = onCancel,
            saveEnabled = currentEntity?.let { isValid(it) } ?: false
        )
    }
}

// Ejemplo de uso para Pacientes
@Composable
fun <T> GenericSearchList(
    items: List<T>,
    onItemClick: (T) -> Unit,
    itemToString: (T) -> String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        GenericTextField(
            label = "Buscar",
            value = searchQuery,
            onValueChange = onSearchQueryChange
        )

        Spacer(modifier = Modifier.height(8.dp))

        val filteredItems = items.filter {
            itemToString(it).contains(searchQuery, ignoreCase = true)
        }

        filteredItems.forEach { item ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                onClick = { onItemClick(item) }
            ) {
                Text(
                    text = itemToString(item),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}