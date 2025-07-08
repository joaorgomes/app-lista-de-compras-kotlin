package com.dispmoveis.listadecompras.dao // Ajuste o pacote conforme sua estrutura

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.dispmoveis.listadecompras.model.ShoppingItem // Importe sua entidade
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingItemDao {

    @Insert
    suspend fun insertShoppingItem(shoppingItem: ShoppingItem)

    @Insert
    suspend fun insertShoppingItems(shoppingItems: List<ShoppingItem>) // Para inserir múltiplos itens

    @Update
    suspend fun updateShoppingItem(shoppingItem: ShoppingItem)

    @Update
    suspend fun updateShoppingItems(shoppingItems: List<ShoppingItem>) // Para atualizar múltiplos itens

    @Delete
    suspend fun deleteShoppingItem(shoppingItem: ShoppingItem)

    @Delete
    suspend fun deleteShoppingItems(shoppingItems: List<ShoppingItem>) // Para deletar múltiplos itens

    @Delete
    suspend fun deleteItems(items: List<ShoppingItem>)



    // Consulta para obter todos os itens de uma lista específica, ordenados por nome
    @Query("SELECT * FROM shopping_items WHERE listId = :listId ORDER BY name ASC")
    fun getItemsForShoppingList(listId: String): Flow<List<ShoppingItem>> // Flow para observação

    // Consulta para obter um item específico pelo ID
    @Query("SELECT * FROM shopping_items WHERE id = :itemId")
    suspend fun getShoppingItemById(itemId: String): ShoppingItem?

    // Consulta para obter o total de itens e itens completos para uma lista (para contadores)
    @Query("SELECT COUNT(id) FROM shopping_items WHERE listId = :listId")
    suspend fun getTotalItemsForList(listId: String): Int

    @Query("SELECT COUNT(id) FROM shopping_items WHERE listId = :listId AND isPurchased = 1")
    suspend fun getCompletedItemsForList(listId: String): Int

    // Consulta para deletar todos os itens de uma lista (útil ao deletar uma lista)
    // Embora ForeignKey.CASCADE faça isso, pode ser útil para debug ou outras lógicas
    @Query("DELETE FROM shopping_items WHERE listId = :listId")
    suspend fun deleteItemsByListId(listId: String)

    @Query("DELETE FROM shopping_items WHERE listId = :listId")
    suspend fun deleteAllItemsForList(listId: String)




}