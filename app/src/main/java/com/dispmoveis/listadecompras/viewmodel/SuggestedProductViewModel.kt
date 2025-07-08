package com.dispmoveis.listadecompras.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dispmoveis.listadecompras.model.SuggestedProduct
import com.dispmoveis.listadecompras.repository.ShoppingListRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.asLiveData

class SuggestedProductViewModel(private val repository: ShoppingListRepository) : ViewModel() {

    // LiveData que observará todas as sugestões do banco de dados
    val allSuggestedProducts = repository.getAllSuggestedProducts().asLiveData()

    fun insert(suggestedProduct: SuggestedProduct) {
        viewModelScope.launch {
            repository.insertSuggestedProduct(suggestedProduct)
        }
    }

    fun delete(suggestedProduct: SuggestedProduct) {
        viewModelScope.launch {
            repository.deleteSuggestedProduct(suggestedProduct)
        }
    }
}

// Factory para instanciar o ViewModel com o repositório
class SuggestedProductViewModelFactory(private val repository: ShoppingListRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SuggestedProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SuggestedProductViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}