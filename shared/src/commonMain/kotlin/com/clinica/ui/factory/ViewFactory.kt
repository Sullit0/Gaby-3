package com.clinica.ui.factory

import androidx.compose.runtime.Composable
import com.benasher44.uuid.Uuid

// Enum para tipos de entidades
enum class EntityType {
    PATIENT,
    SESSION,
    NOTE,
    ATTACHMENT,
    ANALYSIS,
    TASK
}

// Factory para crear vistas dinámicamente
object ViewFactory {

    @Composable
    fun getFormScreen(
        entityType: EntityType,
        entityId: Uuid?,
        onSaved: () -> Unit,
        onCancelled: () -> Unit
    ) {
        when (entityType) {
            EntityType.PATIENT -> {
                // Implementar formulario de paciente usando componentes genéricos
            }
            EntityType.SESSION -> {
                // Implementar formulario de sesión usando componentes genéricos
            }
            EntityType.NOTE -> {
                // Implementar formulario de nota usando componentes genéricos
            }
            EntityType.ATTACHMENT -> {
                // Implementar formulario de adjunto usando componentes genéricos
            }
            EntityType.ANALYSIS -> {
                // Implementar formulario de análisis usando componentes genéricos
            }
            EntityType.TASK -> {
                // Implementar formulario de tarea usando componentes genéricos
            }
        }
    }

    @Composable
    fun getListScreen(
        entityType: EntityType,
        onItemSelected: (Uuid) -> Unit,
        onAddNew: () -> Unit
    ) {
        when (entityType) {
            EntityType.PATIENT -> {
                // Implementar lista de pacientes usando componentes genéricos
            }
            EntityType.SESSION -> {
                // Implementar lista de sesiones usando componentes genéricos
            }
            EntityType.NOTE -> {
                // Implementar lista de notas usando componentes genéricos
            }
            EntityType.ATTACHMENT -> {
                // Implementar lista de adjuntos usando componentes genéricos
            }
            EntityType.ANALYSIS -> {
                // Implementar lista de análisis usando componentes genéricos
            }
            EntityType.TASK -> {
                // Implementar lista de tareas usando componentes genéricos
            }
        }
    }

    fun getTitle(entityType: EntityType): String {
        return when (entityType) {
            EntityType.PATIENT -> "Pacientes"
            EntityType.SESSION -> "Sesiones"
            EntityType.NOTE -> "Notas"
            EntityType.ATTACHMENT -> "Adjuntos"
            EntityType.ANALYSIS -> "Análisis"
            EntityType.TASK -> "Tareas"
        }
    }
}

// Data class para configuración de módulos
data class ModuleConfig(
    val entityType: EntityType,
    val title: String,
    val icon: String, // Podría ser un enum de iconos
    val enabled: Boolean = true
)

// Lista de módulos disponibles
val availableModules = listOf(
    ModuleConfig(EntityType.PATIENT, "Pacientes", "person"),
    ModuleConfig(EntityType.SESSION, "Sesiones", "calendar"),
    ModuleConfig(EntityType.NOTE, "Notas Evolutivas", "note"),
    ModuleConfig(EntityType.ATTACHMENT, "Adjuntos", "attachment"),
    ModuleConfig(EntityType.ANALYSIS, "Análisis", "analytics"),
    ModuleConfig(EntityType.TASK, "Tareas", "task")
)