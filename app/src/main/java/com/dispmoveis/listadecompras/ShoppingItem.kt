package com.dispmoveis.listadecompras

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ShoppingItem(
    val id: Long = System.currentTimeMillis(), // ID único, útil para remoções
    val name: String,
    var quantity: Int = 1, // Adicionado quantidade
    var customQuantityText: String? = null, // Para "2kg", "1 lata", etc.
    var price: Double = 0.0, // Adicionado preço
    var isPurchased: Boolean = false
) : Parcelable