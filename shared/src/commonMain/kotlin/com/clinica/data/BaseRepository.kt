package com.clinica.data

import com.benasher44.uuid.Uuid

// Interfaz base para todos los repositories
interface BaseRepository<T> {
    suspend fun getById(id: Uuid): T?
    suspend fun getAll(): List<T>
    suspend fun insert(entity: T): Boolean
    suspend fun update(entity: T): Boolean
    suspend fun delete(id: Uuid): Boolean
}

// Interfaz para entidades con relaciones
interface RelatedRepository<T, R> : BaseRepository<T> {
    suspend fun getByRelatedId(relatedId: Uuid): List<T>
    suspend fun deleteByRelatedId(relatedId: Uuid): Boolean
}

// Helper para operaciones comunes
abstract class BaseRepositoryHelper<T>(
    private val tableName: String
) {
    protected fun generateId(): Uuid = Uuid.randomUUID()

    protected fun validateEntity(entity: T?): Boolean {
        return entity != null
    }

    protected fun logOperation(operation: String, id: Uuid) {
        println("$operation $tableName with id: $id")
    }
}