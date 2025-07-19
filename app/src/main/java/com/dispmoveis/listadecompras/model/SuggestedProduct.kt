package com.dispmoveis.listadecompras.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * Data class que representa uma lista de sugestoes de itens.
 */

@Parcelize
@Entity(tableName = "suggested_products") // Nome da tabela
data class SuggestedProduct(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String
) : Parcelable