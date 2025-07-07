package com.dispmoveis.listadecompras

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializa ThreeTenABP o mais cedo possível
        AndroidThreeTen.init(this)
    }
}