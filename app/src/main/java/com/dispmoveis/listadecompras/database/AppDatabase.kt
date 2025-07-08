package com.dispmoveis.listadecompras.database // Ajuste o pacote conforme sua estrutura

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters // Importar TypeConverters
import com.dispmoveis.listadecompras.model.ShoppingList // Importar suas entidades
import com.dispmoveis.listadecompras.model.ShoppingItem
import com.dispmoveis.listadecompras.dao.ShoppingListDao // Importar seus DAOs
import com.dispmoveis.listadecompras.dao.ShoppingItemDao
import com.dispmoveis.listadecompras.dao.SuggestedProductDao
import com.dispmoveis.listadecompras.model.SuggestedProduct
import com.dispmoveis.listadecompras.utils.Converters // NOVO: Importar a classe Converters

@Database(
    entities = [ShoppingList::class, ShoppingItem::class, SuggestedProduct::class], // Suas entidades
    version = 1, // Versão do banco de dados. Incremente se mudar o esquema.
    exportSchema = false // Recomendado para apps pequenos/desenvolvimento
)
@TypeConverters(Converters::class) // NOVO: Anotar para usar os TypeConverters
abstract class AppDatabase : RoomDatabase() {

    // Métodos abstratos para acessar seus DAOs
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun suggestedProductDao(): SuggestedProductDao

    companion object {
        @Volatile // Garante que a instância seja sempre a mais recente e visível para todas as threads
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // Se a instância já existe, retorna-a
            return INSTANCE ?: synchronized(this) {
                // Se não existe, cria uma nova instância de forma thread-safe
                val instance = Room.databaseBuilder(
                    context.applicationContext, // Use applicationContext para evitar vazamentos de memória
                    AppDatabase::class.java,
                    "shopping_list_db" // Nome do arquivo do banco de dados
                )
                    //.addMigrations(MIGRATION_1_2) // Adicione aqui se precisar de migrações futuras
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}