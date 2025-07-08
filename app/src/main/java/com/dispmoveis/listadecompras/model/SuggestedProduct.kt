package com.dispmoveis.listadecompras.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
@Entity(tableName = "suggested_products") // Nome da nova tabela
data class SuggestedProduct(
    @PrimaryKey val id: String = UUID.randomUUID().toString(), // ID único para cada sugestão
    val name: String // O nome do produto sugerido
) : Parcelable