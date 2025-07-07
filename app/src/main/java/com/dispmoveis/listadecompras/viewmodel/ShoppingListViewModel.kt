package com.dispmoveis.listadecompras.viewmodel // Ajuste o pacote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData // Para converter Flow em LiveData
import androidx.lifecycle.viewModelScope
import com.dispmoveis.listadecompras.ShoppingList
import com.dispmoveis.listadecompras.repository.ShoppingListRepository
import kotlinx.coroutines.launch

class ShoppingListViewModel(private val repository: ShoppingListRepository) : ViewModel() {

    // Exponha todas as listas de compras como LiveData, observando o Flow do repositório
    val allShoppingLists = repository.allShoppingLists.asLiveData()

    // Função para inserir uma nova lista de compras
    fun insert(shoppingList: ShoppingList) = viewModelScope.launch {
        repository.insertShoppingList(shoppingList)
    }

    // Função para atualizar uma lista de compras existente
    fun update(shoppingList: ShoppingList) = viewModelScope.launch {
        repository.updateShoppingList(shoppingList)
    }

    // Função para deletar uma lista de compras
    /*fun delete(shoppingList: ShoppingList) = viewModelScope.launch {
        repository.deleteShoppingList(shoppingList)
    }*/
    fun delete(shoppingList: ShoppingList) {
        viewModelScope.launch {
            // Com ForeignKey.CASCADE, o Room deletará os itens automaticamente.
            repository.deleteShoppingList(shoppingList)
        }
    }

    // Função para obter uma lista pelo ID (usada talvez para verificar existência ou detalhes)
    suspend fun getShoppingListById(listId: String): ShoppingList? {
        return repository.getShoppingListById(listId)
    }
}

// Factory para instanciar o ViewModel com um construtor personalizado
// O ViewModelProvider precisa de um Factory quando o ViewModel tem parâmetros no construtor
class ShoppingListViewModelFactory(private val repository: ShoppingListRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppingListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShoppingListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}