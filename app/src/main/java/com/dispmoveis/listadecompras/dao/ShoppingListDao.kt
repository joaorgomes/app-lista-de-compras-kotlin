package com.dispmoveis.listadecompras.dao // Ajuste o pacote conforme sua estrutura

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.dispmoveis.listadecompras.ShoppingList // Importe sua entidade
import kotlinx.coroutines.flow.Flow // Importe Flow para observar mudanças

@Dao
interface ShoppingListDao {

    @Insert
    suspend fun insertShoppingList(shoppingList: ShoppingList) // `suspend` para ser chamada de uma coroutine

    @Update
    suspend fun updateShoppingList(shoppingList: ShoppingList)

    @Delete
    suspend fun deleteShoppingList(shoppingList: ShoppingList)

    // Consulta para obter todas as listas de compras, ordenadas por data (ou nome)
    // Usamos Flow para que a UI seja automaticamente atualizada quando houver mudanças.
    @Query("SELECT * FROM shopping_lists ORDER BY date DESC, name ASC")
    fun getAllShoppingLists(): Flow<List<ShoppingList>> // `Flow` não precisa de `suspend`

    // Consulta para obter uma lista específica pelo ID
    @Query("SELECT * FROM shopping_lists WHERE id = :listId")
    suspend fun getShoppingListById(listId: String): ShoppingList? // Pode retornar null
}