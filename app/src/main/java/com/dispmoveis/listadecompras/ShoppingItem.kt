package com.dispmoveis.listadecompras

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ShoppingItem(
    val name: String,
    var isPurchased: Boolean = false
) : Parcelable