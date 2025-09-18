package com.clinica.domain.model

import kotlinx.datetime.Instant

data class Attachment(
    val id: String,
    val sessionId: String,
    val displayName: String,
    val storedName: String,
    val mimeType: String?,
    val sizeBytes: Long?,
    val sha256: String?,
    val createdAt: Instant
)
