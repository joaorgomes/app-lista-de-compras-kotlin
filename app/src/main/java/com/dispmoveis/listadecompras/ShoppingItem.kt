package com.dispmoveis.listadecompras

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import java.util.UUID

@Parcelize
@Entity(
    tableName = "shopping_items",
    foreignKeys = [ForeignKey(
        entity = ShoppingList::class,
        parentColumns = ["id"],
        childColumns = ["listId"],
        ForeignKey.CASCADE
    )]
)
data class ShoppingItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val listId: String,
    val name: String,
    var quantity: Int = 1,
    var customQuantityText: String? = null,
    var price: Double = 0.0,
    var isPurchased: Boolean = false
) : Parcelable