package com.dispmoveis.listadecompras.utils

import androidx.room.TypeConverter
import org.threeten.bp.Instant // MUDOU AQUI
import org.threeten.bp.LocalDate // MUDOU AQUI
import org.threeten.bp.ZoneId // MUDOU AQUI
import org.threeten.bp.ZoneOffset // Pode ser útil, se precisar de UTC

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDate? {
        return value?.let {
            // Usa as classes do ThreeTenABP
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): Long? {
        return date?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    }
}