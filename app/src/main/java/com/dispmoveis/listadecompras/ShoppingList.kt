package com.dispmoveis.listadecompras

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.UUID
import org.threeten.bp.LocalDate

/**
 * Data class que representa uma lista de compras individual.
 */
@Parcelize
@Entity(tableName = "shopping_lists")
data class ShoppingList(
    @PrimaryKey // NOVO: Define 'id' como a chave primária
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var date: org.threeten.bp.LocalDate? = null,
    var isArchived: Boolean = false,
    var totalItems: Int = 0,
    var completedItems: Int = 0
): Parcelable