package com.clinica.app.form

import com.clinica.domain.model.Patient
import com.clinica.domain.model.Session
import com.clinica.domain.model.SessionTasks
import com.clinica.domain.model.Attachment

data class SessionFormState(
    val patient: Patient? = null,
    val session: Session? = null,
    val tasks: SessionTasks? = null,
    val attachments: List<Attachment> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
