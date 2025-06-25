package com.dispmoveis.listadecompras

data class ShoppingItem (
    val name: String,
    var isPurchased: Boolean = false // Indica se o item foi comprado
    )