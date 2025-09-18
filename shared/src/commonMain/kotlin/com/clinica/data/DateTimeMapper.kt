package com.clinica.data

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

object DateTimeMapper {
    fun stringToLocalDate(value: String?): LocalDate? =
        value?.let { LocalDate.parse(it) }

    fun localDateToString(value: LocalDate?): String? = value?.toString()

    fun stringToInstant(value: String?): Instant? = value?.let { Instant.parse(it) }

    fun instantToString(value: Instant): String = value.toString()
}
