package com.dispmoveis.listadecompras.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dispmoveis.listadecompras.model.SuggestedProduct
import kotlinx.coroutines.flow.Flow

@Dao
interface SuggestedProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) // Se tentar inserir uma sugestão com ID existente, ela será substituída
    suspend fun insert(suggestedProduct: SuggestedProduct)

    @Delete
    suspend fun delete(suggestedProduct: SuggestedProduct)

    @Query("SELECT * FROM suggested_products ORDER BY name ASC") // Obtém todas as sugestões em ordem alfabética
    fun getAllSuggestedProducts(): Flow<List<SuggestedProduct>> // Retorna um Flow para observação reativa
}