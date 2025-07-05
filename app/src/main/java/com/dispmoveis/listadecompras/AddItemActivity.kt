package com.dispmoveis.listadecompras

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu // NOVO: Import para Menu
import android.view.MenuItem // NOVO: Import para MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast

import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.dispmoveis.listadecompras.databinding.ActivityAddItemBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

class AddItemActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddItemBinding

    private val allSuggestedNames = mutableListOf<String>()
    private val filteredSuggestedNames = mutableListOf<String>()

    private val selectedShoppingItems = mutableListOf<ShoppingItem>()
    private val originalExistingItems = mutableListOf<ShoppingItem>()

    private lateinit var suggestedProductAdapter: SuggestedProductAdapter
    private lateinit var selectedProductAdapter: SelectedProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAddItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // REMOVIDO: ViewCompat.setOnApplyWindowInsetsListener daqui
        // Com fitsSystemWindows="true" no CoordinatorLayout no XML, o sistema lida com os insets automaticamente.
        // O enableEdgeToEdge() já estende o conteúdo por trás das barras, e fitsSystemWindows garante o padding.

        setSupportActionBar(binding.toolbarAddItem)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.add_new_item_title)
        }
        binding.toolbarAddItem.setNavigationOnClickListener {
            // Ao clicar na seta de voltar, também finalizamos e retornamos os itens
            onFinishAddingItems()
        }

        val existingItemsFromDetail = intent.getParcelableArrayListExtra<ShoppingItem>("EXISTING_SHOPPING_ITEMS")
        existingItemsFromDetail?.let {
            Log.d("AddItemActivity", "DEBUG_ONCREATE: Itens recebidos para inicialização: ${it.size}")
            selectedShoppingItems.addAll(it)
            originalExistingItems.addAll(it.map { item -> item.copy() })
            Log.d("AddItemActivity", "DEBUG_ONCREATE: selectedShoppingItems após adicionar existentes: ${selectedShoppingItems.size}")
            Log.d("AddItemActivity", "DEBUG_ONCREATE: originalExistingItems populado com ${originalExistingItems.size} itens.")
        }

        populateSuggestedItems()

        setupSelectedItemsRecyclerView()
        setupSuggestedItemsRecyclerView()

        setupSearchInput()
        // REMOVIDO: setupFinishButton() // Não precisamos mais deste método pois o botão foi para a Toolbar
        setupAddNewSuggestionFab()

        updateSelectedItemsUI()
        filterSuggestedItems(binding.editTextSearchItem.text.toString())
    }

    // NOVO: Inflar o menu na Toolbar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_add_item, menu) // Assegure-se de que menu_add_item.xml existe
        return true
    }

    // NOVO: Lidar com cliques nos itens do menu da Toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_finish_add_items -> { // Assegure-se de que este ID está em menu_add_item.xml
                onFinishAddingItems()
                true
            }
            // Não precisa de um case para android.R.id.home (a seta de voltar), pois já configuramos
            // o setNavigationOnClickListener no onCreate para chamar onFinishAddingItems().
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Método que centraliza a lógica de finalizar e retornar os itens
    private fun onFinishAddingItems() {
        val resultIntent = Intent()
        Log.d("AddItemActivity", "DEBUG_FINISH: Retornando ${selectedShoppingItems.size} itens para a Detail Activity.")
        selectedShoppingItems.forEachIndexed { index, item ->
            Log.d("AddItemActivity", "DEBUG_FINISH: Item a retornar[$index]: Name='${item.name}', Qtd=${item.quantity}, Purchased=${item.isPurchased}, ID=${item.id}")
        }
        resultIntent.putParcelableArrayListExtra("SELECTED_SHOPPING_ITEMS_RESULT", ArrayList(selectedShoppingItems))
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }


    // Sobrescreve o método onBackPressed para garantir que o retorno seja feito
    override fun onBackPressed() {
        // Quando o usuário pressiona o botão Voltar, também finalizamos e retornamos os itens
        onFinishAddingItems() // Chama a função centralizada
        // super.onBackPressed() não é necessário aqui porque onFinishAddingItems já chama finish()
    }

    // MÉTODO para determinar o que foi alterado/adicionado/removido
    // Mantido por enquanto, mas a lógica de retorno direto da lista completa é a que será usada.
    private fun getChangedOrNewAndRemovedItems(): ArrayList<ShoppingItem> {
        val finalResultList = ArrayList<ShoppingItem>()

        // 1. Itens Adicionados ou Modificados
        selectedShoppingItems.forEach { currentItem ->
            val originalItem = originalExistingItems.find { it.id == currentItem.id }

            if (originalItem == null) {
                // Este é um item completamente NOVO, que não existia na lista original
                finalResultList.add(currentItem)
                Log.d("AddItemActivity", "DEBUG_RETURN: Item NOVO detectado: '${currentItem.name}', Qtd: ${currentItem.quantity}, ID: ${currentItem.id}")
            } else {
                // É um item existente. Verificar se a quantidade (ou outros atributos) mudou.
                if (currentItem.quantity != originalItem.quantity ||
                    currentItem.isPurchased != originalItem.isPurchased ||
                    currentItem.name != originalItem.name ||
                    currentItem.customQuantityText != originalItem.customQuantityText) {
                    // Adiciona o item com as novas propriedades
                    finalResultList.add(currentItem)
                    Log.d("AddItemActivity", "DEBUG_RETURN: Item EXISTENTE com Qtd/Status alterado: '${currentItem.name}', Qtd original: ${originalItem.quantity}, Qtd nova: ${currentItem.quantity}, ID: ${currentItem.id}")
                }
            }
        }

        // 2. Itens Removidos (Esta parte não está adicionando ao finalResultList
        // para o retorno, pois a lógica é para a ShoppingListDetailActivity conciliar)
        originalExistingItems.forEach { originalItem ->
            val currentItem = selectedShoppingItems.find { it.id == originalItem.id }
            if (currentItem == null) {
                Log.d("AddItemActivity", "DEBUG_RETURN: Item ORIGINAL removido: '${originalItem.name}', ID: ${originalItem.id}")
            }
        }
        return ArrayList(selectedShoppingItems) // Retorna a lista final de itens selecionados/modificados
    }

    private fun setupSelectedItemsRecyclerView() {
        selectedProductAdapter = SelectedProductAdapter(
            onQuantityChange = { item ->
                Log.d("AddItemActivity", "DEBUG_CALLBACK: Qtd '${item.name}' alterada para ${item.quantity}. selectedShoppingItems size: ${selectedShoppingItems.size}")
                updateSelectedItemsUI()
            },
            onRemoveClick = { itemToRemove ->
                Log.d("AddItemActivity", "DEBUG_CALLBACK: Tentando remover '${itemToRemove.name}'. selectedShoppingItems size ANTES: ${selectedShoppingItems.size}")
                val removed = selectedShoppingItems.remove(itemToRemove)
                if (removed) {
                    Log.d("AddItemActivity", "DEBUG_CALLBACK: Item '${itemToRemove.name}' REMOVIDO. selectedShoppingItems size DEPOIS: ${selectedShoppingItems.size}")

                    val wasOriginalItem = originalExistingItems.any { it.id == itemToRemove.id }
                    if (!wasOriginalItem && !allSuggestedNames.contains(itemToRemove.name)) {
                        allSuggestedNames.add(itemToRemove.name)
                        allSuggestedNames.sort()
                        Log.d("AddItemActivity", "DEBUG_CALLBACK: '${itemToRemove.name}' adicionado de volta às sugestões base.")
                    } else if (wasOriginalItem) {
                        Log.d("AddItemActivity", "DEBUG_CALLBACK: '${itemToRemove.name}' era um item original, não adicionado de volta às sugestões.")
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
                    val newItem = ShoppingItem(name = productName, quantity = 1, isPurchased = false)
                    selectedShoppingItems.add(newItem)
                    Log.d("AddItemActivity", "DEBUG_ONADD: Adicionado novo item sugerido: ${productName}, Qtd: ${newItem.quantity}.")
                }

                Log.d("AddItemActivity", "DEBUG_ONADD: selectedShoppingItems size DEPOIS da adição/incremento: ${selectedShoppingItems.size}")

                allSuggestedNames.remove(productName)
                Log.d("AddItemActivity", "DEBUG_ONADD: '$productName' removido de allSuggestedNames. AllSuggestedNames size: ${allSuggestedNames.size}")

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
        etItemQuantity.visibility = View.GONE

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

                if (allSuggestedNames.any { it.equals(newItemName, ignoreCase = true) }) {
                    Toast.makeText(this, "Sugestão '${newItemName}' já existe nas sugestões.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (selectedShoppingItems.any { it.name.equals(newItemName, ignoreCase = true) }) {
                    Toast.makeText(this, "Item '${newItemName}' já está na sua lista selecionada. Você pode adicioná-lo por lá.", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                    return@setPositiveButton
                }

                allSuggestedNames.add(newItemName)
                allSuggestedNames.sortWith(Comparator { s1, s2 ->
                    s1.lowercase(Locale.ROOT).compareTo(s2.lowercase(Locale.ROOT))
                })

                val newItem = ShoppingItem(name = newItemName, quantity = 1, isPurchased = false)
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


    private fun populateSuggestedItems() {
        allSuggestedNames.addAll(listOf(
            "Alface", "Arroz", "Atum", "Açúcar", "Banana", "Batata",
            "Carne bovina", "Carne frango", "Cebola", "Cenoura", "Cereal",
            "Café", "Leite", "Pão", "Ovos", "Salsicha", "Tomate", "Macarrão",
            "Chocolate", "Refrigerante", "Feijão", "Abacaxi", "Cerveja"
        ))
        allSuggestedNames.sort()
        Log.d("AddItemActivity", "DEBUG_POPULATE: Sugestões iniciais populadas. Total: ${allSuggestedNames.size}")
    }

    private fun filterSuggestedItems(query: String) {
        filteredSuggestedNames.clear()
        val lowerCaseQuery = query.lowercase(Locale.ROOT)
        val currentSelectedNames = selectedShoppingItems.map { it.name.lowercase(Locale.ROOT) }.toSet()

        Log.d("AddItemActivity", "DEBUG_FILTER: Filtrando sugestões para query: '$query'. Itens selecionados atualmente para exclusão: ${currentSelectedNames.size}")

        for (item in allSuggestedNames) {
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
        selectedShoppingItems.forEachIndexed { index, item ->
            Log.d("AddItemActivity", "DEBUG_UI: selectedShoppingItems[$index]: Name='${item.name}', Qtd=${item.quantity}")
        }

        if (selectedShoppingItems.isEmpty()) {
            binding.textViewSelectedItemsTitle.visibility = View.GONE
            binding.recyclerViewSelectedItems.visibility = View.GONE
            Log.d("AddItemActivity", "DEBUG_UI: selectedShoppingItems VAZIA. Escondendo UI.")
        } else {
            binding.textViewSelectedItemsTitle.visibility = View.VISIBLE
            binding.recyclerViewSelectedItems.visibility = View.VISIBLE
            Log.d("AddItemActivity", "DEBUG_UI: selectedShoppingItems NÃO VAZIA. Mostrando UI.")
        }
        selectedProductAdapter.updateList(selectedShoppingItems)
        Log.d("AddItemActivity", "DEBUG_UI: Chamado selectedProductAdapter.updateList com ${selectedShoppingItems.size} itens (depois da chamada ao adapter).")
    }
}