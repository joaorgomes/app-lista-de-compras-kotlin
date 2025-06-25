package com.dispmoveis.listadecompras
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProductItem (
    val name: String,
    var quantity: Int, // Quantidade do item (ex: 1, 2, etc.)
    // Adicionaremos mais campos aqui se o usuário digitar quantidade personalizada (ex: "2kg")
    // Por enquanto, 'quantity' será um Int.
    var customQuantityText: String? = null // Para "2kg", "1 lata", etc.
): Parcelable