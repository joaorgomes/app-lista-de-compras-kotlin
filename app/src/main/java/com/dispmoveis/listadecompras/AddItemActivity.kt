package com.dispmoveis.listadecompras

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.dispmoveis.listadecompras.databinding.ActivityAddItemBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale
import java.util.UUID // Importe UUID

class AddItemActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddItemBinding

    // Nomes base de sugestões que podemos popular
    private val allBaseSuggestedNames = mutableListOf<String>()
    // Nomes de sugestões filtrados para exibição
    private val filteredSuggestedNames = mutableListOf<String>()

    // Esta lista vai conter os itens que o usuário SELECIONOU/MODIFICOU nesta tela
    // Inclui os que vieram da lista original e os novos
    private val selectedShoppingItems = mutableListOf<ShoppingItem>()

    // Mantém uma cópia dos itens que JÁ EXISTIAM na lista quando esta Activity foi aberta
    private val originalExistingItems = mutableListOf<ShoppingItem>()

    private lateinit var suggestedProductAdapter: SuggestedProductAdapter
    private lateinit var selectedProductAdapter: SelectedProductAdapter

    private var currentListId: String? = null // Receberemos o ID da lista da DetailActivity

    companion object {
        const val EXTRA_SELECTED_SHOPPING_ITEMS_RESULT = "SELECTED_SHOPPING_ITEMS_RESULT"
        const val EXTRA_EXISTING_SHOPPING_ITEMS = "EXISTING_SHOPPING_ITEMS" // Itens que JÁ ESTÃO na lista
        const val EXTRA_CURRENT_LIST_ID = "CURRENT_LIST_ID" // ID da lista para onde os itens irão
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarAddItem)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.add_new_item_title)
        }
        binding.toolbarAddItem.setNavigationOnClickListener {
            onFinishAddingItems() // Ao clicar na seta de voltar, finalizamos e retornamos
        }

        // 1. Receber o ID da lista e os itens EXISTENTES
        currentListId = intent.getStringExtra(EXTRA_CURRENT_LIST_ID)
        val existingItemsFromDetail = intent.getParcelableArrayListExtra<ShoppingItem>(EXTRA_EXISTING_SHOPPING_ITEMS)

        existingItemsFromDetail?.let {
            Log.d("AddItemActivity", "DEBUG_ONCREATE: Itens EXISTENTES recebidos: ${it.size}")
            originalExistingItems.addAll(it) // Salva os itens originais
            selectedShoppingItems.addAll(it.map { item -> item.copy() }) // Adiciona cópias para a lista de trabalho
            Log.d("AddItemActivity", "DEBUG_ONCREATE: selectedShoppingItems inicializado com ${selectedShoppingItems.size} itens (incluindo existentes).")
        }

        populateBaseSuggestedNames() // Popula a lista de nomes de sugestão base

        setupSelectedItemsRecyclerView()
        setupSuggestedItemsRecyclerView()

        setupSearchInput()
        setupAddNewSuggestionFab()

        updateSelectedItemsUI() // Atualiza a UI de itens selecionados inicialmente
        filterSuggestedItems(binding.editTextSearchItem.text.toString()) // Filtra sugestões inicialmente
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_add_item, menu) // Assegure-se de que menu_add_item.xml existe
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_finish_add_items -> { // Assegure-se de que este ID está em menu_add_item.xml
                onFinishAddingItems()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Centraliza a lógica de finalizar e retornar os itens
    private fun onFinishAddingItems() {
        val resultIntent = Intent()
        Log.d("AddItemActivity", "DEBUG_FINISH: Retornando ${selectedShoppingItems.size} itens para a Detail Activity.")
        resultIntent.putParcelableArrayListExtra(EXTRA_SELECTED_SHOPPING_ITEMS_RESULT, ArrayList(selectedShoppingItems))
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    // Sobrescreve o método onBackPressed para garantir que o retorno seja feito
    override fun onBackPressed() {
        onFinishAddingItems()
        //Super.onBackPressed() //é chamado implicitamente por finish()
    }

    private fun setupSelectedItemsRecyclerView() {
        selectedProductAdapter = SelectedProductAdapter(
            onQuantityChange = { item ->
                Log.d("AddItemActivity", "DEBUG_CALLBACK: Qtd '${item.name}' alterada para ${item.quantity}. selectedShoppingItems size: ${selectedShoppingItems.size}")
                updateSelectedItemsUI()
            },
            onRemoveClick = { itemToRemove ->
                Log.d("AddItemActivity", "DEBUG_CALLBACK: Tentando remover '${itemToRemove.name}'. selectedShoppingItems size ANTES: ${selectedShoppingItems.size}")
                val removed = selectedShoppingItems.remove(itemToRemove) // Remove a instância correta

                if (removed) {
                    Log.d("AddItemActivity", "DEBUG_CALLBACK: Item '${itemToRemove.name}' REMOVIDO. selectedShoppingItems size DEPOIS: ${selectedShoppingItems.size}")

                    // Se o item removido NÃO era um item original e NÃO é uma sugestão base, adiciona de volta às sugestões.
                    // Isso evita que itens existentes (originais) reapareçam nas sugestões após serem removidos da seleção.
                    val isOriginalItem = originalExistingItems.any { it.id == itemToRemove.id }
                    if (!isOriginalItem && !allBaseSuggestedNames.contains(itemToRemove.name)) {
                        allBaseSuggestedNames.add(itemToRemove.name)
                        allBaseSuggestedNames.sort()
                        Log.d("AddItemActivity", "DEBUG_CALLBACK: '${itemToRemove.name}' adicionado de volta às sugestões base.")
                    } else if (isOriginalItem) {
                        Log.d("AddItemActivity", "DEBUG_CALLBACK: '${itemToRemove.name}' era um item original. Não adicionado de volta às sugestões para evitar duplicação em casos de sugestões base.")
                    }
                    updateSelectedItemsUI()
                    filterSuggestedItems(binding.editTextSearchItem.text.toString())
                } else {
                    Log.d("AddItemActivity", "DEBUG_CALLBACK: Item '${itemToRemove.name}' NÃO encontrado para remoção.")
                }
            }
        )
        selectedProductAdapter.updateList(selectedShoppingItems)
        binding.recyclerViewSelectedItems.apply {
            layoutManager = LinearLayoutManager(this@AddItemActivity)
            adapter = selectedProductAdapter
        }
        Log.d("AddItemActivity", "DEBUG_SETUP: SelectedProductAdapter configurado com selectedShoppingItems size: ${selectedShoppingItems.size}")
    }

    private fun setupSuggestedItemsRecyclerView() {
        suggestedProductAdapter = SuggestedProductAdapter(
            filteredSuggestedNames,
            onAddClick = { productName ->
                Log.d("AddItemActivity", "DEBUG_ONADD: Clicado no '+' para: $productName. selectedShoppingItems size ANTES: ${selectedShoppingItems.size}")

                val existingItemInSelection = selectedShoppingItems.find { it.name.equals(productName, ignoreCase = true) }

                if (existingItemInSelection != null) {
                    existingItemInSelection.quantity++
                    selectedProductAdapter.notifyItemChanged(selectedShoppingItems.indexOf(existingItemInSelection))
                    Log.d("AddItemActivity", "DEBUG_ONADD: Incrementada quantidade de item existente na seleção: ${productName}, Qtd: ${existingItemInSelection.quantity}")
                } else {
                    // Crie um NOVO ShoppingItem com um ID único e o currentListId
                    val newItem = ShoppingItem(
                        id = UUID.randomUUID().toString(),
                        name = productName,
                        quantity = 1,
                        isPurchased = false,
                        listId = currentListId ?: "" // Usa o ID da lista real
                    )
                    selectedShoppingItems.add(newItem)
                    Log.d("AddItemActivity", "DEBUG_ONADD: Adicionado novo item sugerido: ${productName}, Qtd: ${newItem.quantity}.")
                }

                Log.d("AddItemActivity", "DEBUG_ONADD: selectedShoppingItems size DEPOIS da adição/incremento: ${selectedShoppingItems.size}")

                // Remove o item da lista de sugestões, pois ele agora está na lista de selecionados
                allBaseSuggestedNames.remove(productName)
                Log.d("AddItemActivity", "DEBUG_ONADD: '$productName' removido de allBaseSuggestedNames. AllBaseSuggestedNames size: ${allBaseSuggestedNames.size}")

                filterSuggestedItems(binding.editTextSearchItem.text.toString())
                updateSelectedItemsUI()
            }
        )
        binding.recyclerViewSuggestedItems.apply {
            layoutManager = LinearLayoutManager(this@AddItemActivity)
            adapter = suggestedProductAdapter
        }
        Log.d("AddItemActivity", "DEBUG_SETUP: SuggestedProductAdapter configurado.")
    }

    private fun setupSearchInput() {
        binding.editTextSearchItem.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterSuggestedItems(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupAddNewSuggestionFab() {
        binding.fabAddNewSuggestion.setOnClickListener {
            showAddNewSuggestionDialog()
        }
    }

    private fun showAddNewSuggestionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_item, null)
        val etItemName: EditText = dialogView.findViewById(R.id.editTextItemName)
        val etItemQuantity: EditText = dialogView.findViewById(R.id.editTextItemQuantity)
        etItemQuantity.visibility = View.GONE // Não precisa de quantidade aqui

        val currentSearchQuery = binding.editTextSearchItem.text.toString().trim()
        if (currentSearchQuery.isNotEmpty()) {
            etItemName.setText(currentSearchQuery)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.add_new_suggested_item_dialog_title))
            .setView(dialogView)
            .setPositiveButton("Adicionar") { dialog, _ ->
                val newItemName = etItemName.text.toString().trim()
                if (newItemName.isEmpty()) {
                    Toast.makeText(this, "O nome da sugestão não pode ser vazio.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Verifica se a sugestão já existe na lista base de sugestões
                if (allBaseSuggestedNames.any { it.equals(newItemName, ignoreCase = true) }) {
                    Toast.makeText(this, "Sugestão '${newItemName}' já existe nas sugestões.", Toast.LENGTH_SHORT).show()
                    dialog.dismiss() // Fecha o diálogo
                    return@setPositiveButton
                }

                // Verifica se o item já está na lista de itens selecionados (incluindo os originais)
                if (selectedShoppingItems.any { it.name.equals(newItemName, ignoreCase = true) }) {
                    Toast.makeText(this, "Item '${newItemName}' já está na sua lista selecionada.", Toast.LENGTH_LONG).show()
                    dialog.dismiss() // Fecha o diálogo
                    return@setPositiveButton
                }

                // Adiciona o novo nome à lista de sugestões base para que possa ser filtrado/sugerido no futuro
                allBaseSuggestedNames.add(newItemName)
                allBaseSuggestedNames.sortWith(Comparator { s1, s2 ->
                    s1.lowercase(Locale.ROOT).compareTo(s2.lowercase(Locale.ROOT))
                })

                // Cria o novo ShoppingItem e o adiciona à lista de selecionados
                val newItem = ShoppingItem(
                    id = UUID.randomUUID().toString(), // Novo ID único
                    name = newItemName,
                    quantity = 1,
                    isPurchased = false,
                    listId = currentListId ?: "" // Usa o ID da lista real
                )
                selectedShoppingItems.add(newItem)
                Log.d("AddItemActivity", "DEBUG_FAB: Nova sugestão '${newItemName}' adicionada e selecionada automaticamente.")

                Toast.makeText(this, "Sugestão '${newItemName}' adicionada e selecionada!", Toast.LENGTH_SHORT).show()

                filterSuggestedItems(binding.editTextSearchItem.text.toString())
                updateSelectedItemsUI()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun populateBaseSuggestedNames() {
        allBaseSuggestedNames.addAll(listOf(
            "Alface", "Arroz", "Atum", "Açúcar", "Banana", "Batata",
            "Carne bovina", "Carne frango", "Cebola", "Cenoura", "Cereal",
            "Café", "Leite", "Pão", "Ovos", "Salsicha", "Tomate", "Macarrão",
            "Chocolate", "Refrigerante", "Feijão", "Abacaxi", "Cerveja",
            // Adicione mais sugestões conforme necessário
        ))
        allBaseSuggestedNames.sort()
        Log.d("AddItemActivity", "DEBUG_POPULATE: Sugestões iniciais populadas. Total: ${allBaseSuggestedNames.size}")
    }

    private fun filterSuggestedItems(query: String) {
        filteredSuggestedNames.clear()
        val lowerCaseQuery = query.lowercase(Locale.ROOT)
        // Obtém os nomes dos itens que já estão na lista de selecionados (incluindo os originais)
        val currentSelectedNames = selectedShoppingItems.map { it.name.lowercase(Locale.ROOT) }.toSet()

        Log.d("AddItemActivity", "DEBUG_FILTER: Filtrando sugestões para query: '$query'. Itens selecionados atualmente (para exclusão da sugestão): ${currentSelectedNames.size}")

        for (item in allBaseSuggestedNames) {
            val itemLowerCase = item.lowercase(Locale.ROOT)
            if (itemLowerCase.contains(lowerCaseQuery) && !currentSelectedNames.contains(itemLowerCase)) {
                filteredSuggestedNames.add(item)
            }
        }
        suggestedProductAdapter.updateSuggestions(filteredSuggestedNames)
        Log.d("AddItemActivity", "DEBUG_FILTER: Sugestões filtradas. Total: ${filteredSuggestedNames.size}. Sugeridos exibidos: ${suggestedProductAdapter.itemCount}")
    }

    private fun updateSelectedItemsUI() {
        Log.d("AddItemActivity", "DEBUG_UI: Início de updateSelectedItemsUI. selectedShoppingItems size: ${selectedShoppingItems.size}")

        if (selectedShoppingItems.isEmpty()) {
            binding.textViewSelectedItemsTitle.visibility = View.GONE
            binding.recyclerViewSelectedItems.visibility = View.GONE
            Log.d("AddItemActivity", "DEBUG_UI: selectedShoppingItems VAZIA. Escondendo UI.")
        } else {
            binding.textViewSelectedItemsTitle.visibility = View.VISIBLE
            binding.recyclerViewSelectedItems.visibility = View.VISIBLE
            Log.d("AddItemActivity", "DEBUG_UI: selectedShoppingItems NÃO VAZIA. Mostrando UI.")
        }
        selectedProductAdapter.updateList(selectedShoppingItems.sortedBy { it.name }) // Ordena para melhor visualização
        Log.d("AddItemActivity", "DEBUG_UI: Chamado selectedProductAdapter.updateList com ${selectedShoppingItems.size} itens (depois da chamada ao adapter).")
    }
}