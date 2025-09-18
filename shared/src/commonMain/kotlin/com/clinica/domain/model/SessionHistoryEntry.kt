package com.clinica.domain.model

import kotlinx.datetime.Instant

data class SessionHistoryEntry(
    val id: Long,
    val sessionId: String,
    val changeType: String,
    val changedAt: Instant,
    val payload: String?
)
