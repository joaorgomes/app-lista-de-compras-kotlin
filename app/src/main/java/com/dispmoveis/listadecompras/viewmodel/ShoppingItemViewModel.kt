package com.dispmoveis.listadecompras.viewmodel // Ajuste o pacote

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.dispmoveis.listadecompras.ShoppingItem
import com.dispmoveis.listadecompras.repository.ShoppingListRepository // O mesmo repositório pode lidar com itens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow // Importe Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class ShoppingItemViewModel(private val repository: ShoppingListRepository) : ViewModel() {

    // MutableLiveData para armazenar o ID da lista atual
    // Este ID será definido pela ShoppingListDetailActivity quando ela for iniciada
    private val _currentListId = MutableLiveData<String>()

    // Flow de itens para a lista atual. Será nulo até que _currentListId seja definido.
    val itemsForCurrentList: LiveData<List<ShoppingItem>> = _currentListId.asFlow()
        .flatMapLatest { listId -> // flatMapLatest muda o Flow interno quando _currentListId muda
            repository.getItemsForList(listId)
        }.asLiveData()

    // Função para definir o ID da lista que este ViewModel deve observar
    fun setCurrentListId(listId: String) {
        if (_currentListId.value != listId) { // Evita atualizações desnecessárias
            _currentListId.value = listId
        }
    }

    fun insert(shoppingItem: ShoppingItem) = viewModelScope.launch {
        repository.insertShoppingItem(shoppingItem)
        _currentListId.value?.let { listId ->
            repository.updateListCounters(listId) // Atualiza os contadores da lista pai
        }
    }

    fun insertItems(shoppingItems: List<ShoppingItem>) = viewModelScope.launch {
        repository.insertShoppingItems(shoppingItems)
        _currentListId.value?.let { listId ->
            repository.updateListCounters(listId) // Atualiza os contadores da lista pai
        }
    }

    fun update(shoppingItem: ShoppingItem) = viewModelScope.launch {
        repository.updateShoppingItem(shoppingItem)
        _currentListId.value?.let { listId ->
            repository.updateListCounters(listId) // Atualiza os contadores da lista pai
        }
    }


    fun delete(shoppingItem: ShoppingItem) = viewModelScope.launch {
        repository.deleteShoppingItem(shoppingItem)
        _currentListId.value?.let { listId ->
            repository.updateListCounters(listId) // Atualiza os contadores da lista pai
        }
    }
    fun deleteItems(items: List<ShoppingItem>) { // NOVO MÉTODO
        viewModelScope.launch {
            repository.deleteShoppingItems(items)
        }
    }

}

// Factory para instanciar o ViewModel com um construtor personalizado
class ShoppingItemViewModelFactory(private val repository: ShoppingListRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppingItemViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShoppingItemViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}