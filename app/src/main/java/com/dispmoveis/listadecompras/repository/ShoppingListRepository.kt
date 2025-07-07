package com.dispmoveis.listadecompras.repository // Ajuste o pacote

import com.dispmoveis.listadecompras.dao.ShoppingListDao
import com.dispmoveis.listadecompras.dao.ShoppingItemDao // Pode ser útil para lógica futura de contadores
import com.dispmoveis.listadecompras.ShoppingList
import com.dispmoveis.listadecompras.ShoppingItem
import kotlinx.coroutines.flow.Flow

// A classe Repository não é @Injectable diretamente (por enquanto, para simplificar)
// Ela vai receber os DAOs no construtor
class ShoppingListRepository(
    private val shoppingListDao: ShoppingListDao,
    private val shoppingItemDao: ShoppingItemDao // Embora este repo seja focado em listas, podemos ter acesso aos itens para contagem, etc.
) {
    // ---- Operações para ShoppingList ----
    val allShoppingLists: Flow<List<ShoppingList>> = shoppingListDao.getAllShoppingLists()

    suspend fun insertShoppingList(shoppingList: ShoppingList) {
        shoppingListDao.insertShoppingList(shoppingList)
    }

    suspend fun updateShoppingList(shoppingList: ShoppingList) {
        shoppingListDao.updateShoppingList(shoppingList)
    }

    suspend fun deleteShoppingList(shoppingList: ShoppingList) {
        // Ao deletar uma lista, seus itens são deletados automaticamente devido ao onDelete = ForeignKey.CASCADE
        // mas podemos adicionar uma lógica aqui se precisar de algo mais
        shoppingListDao.deleteShoppingList(shoppingList)
    }

    suspend fun getShoppingListById(listId: String): ShoppingList? {
        return shoppingListDao.getShoppingListById(listId)
    }


    // ---- Operações para ShoppingItem (relacionadas a contadores ou detalhes de lista) ----

    // Este Flow observará os itens para uma lista específica
    fun getItemsForList(listId: String): Flow<List<ShoppingItem>> {
        return shoppingItemDao.getItemsForShoppingList(listId)
    }

    suspend fun insertShoppingItem(item: ShoppingItem) {
        shoppingItemDao.insertShoppingItem(item)
    }

    suspend fun insertShoppingItems(items: List<ShoppingItem>) {
        shoppingItemDao.insertShoppingItems(items)
    }

    suspend fun updateShoppingItem(item: ShoppingItem) {
        shoppingItemDao.updateShoppingItem(item)
    }

    suspend fun deleteShoppingItem(item: ShoppingItem) {
        shoppingItemDao.deleteShoppingItem(item)
    }
    suspend fun deleteShoppingItems(shoppingItems: List<ShoppingItem>) {
        shoppingItemDao.deleteShoppingItems(shoppingItems)
    }

    suspend fun getShoppingItemById(itemId: String): ShoppingItem? {
        return shoppingItemDao.getShoppingItemById(itemId)
    }


    // Função para atualizar os contadores de uma ShoppingList baseados nos seus ShoppingItems
    suspend fun updateListCounters(listId: String) {
        val total = shoppingItemDao.getTotalItemsForList(listId)
        val completed = shoppingItemDao.getCompletedItemsForList(listId)
        val list = shoppingListDao.getShoppingListById(listId)

        list?.let {
            it.totalItems = total
            it.completedItems = completed
            shoppingListDao.updateShoppingList(it)
        }
    }
}