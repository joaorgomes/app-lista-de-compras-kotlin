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
import androidx.lifecycle.ViewModelProvider // Importe ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.dispmoveis.listadecompras.adapters.SelectedProductAdapter
import com.dispmoveis.listadecompras.adapters.SuggestedProductAdapter
import com.dispmoveis.listadecompras.database.AppDatabase // Importe AppDatabase
import com.dispmoveis.listadecompras.databinding.ActivityAddItemBinding
import com.dispmoveis.listadecompras.model.ShoppingItem // Use o pacote correto para ShoppingItem, se for model
import com.dispmoveis.listadecompras.model.SuggestedProduct // NOVO: Importe a entidade SuggestedProduct
import com.dispmoveis.listadecompras.repository.ShoppingListRepository // Importe ShoppingListRepository
import com.dispmoveis.listadecompras.viewmodel.SuggestedProductViewModel // Importe SuggestedProductViewModel
import com.dispmoveis.listadecompras.viewmodel.SuggestedProductViewModelFactory // Importe SuggestedProductViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.util.Locale
import java.util.UUID

class AddItemActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddItemBinding

    // ESTA LISTA AGORA SERÁ POPULADA PELO VIEWMARX DO BANCO DE DADOS
    private var allSuggestedProductsFromDb: List<SuggestedProduct> = emptyList()

    // Nomes de sugestões filtrados para exibição (usará os nomes de allSuggestedProductsFromDb)
    private val filteredSuggestedNames = mutableListOf<String>()

    // Esta lista vai conter os itens que o usuário SELECIONOU/MODIFICOU nesta tela
    // Inclui os que vieram da lista original e os novos
    private val selectedShoppingItems = mutableListOf<ShoppingItem>()

    // Mantém uma cópia dos itens que JÁ EXISTIAM na lista quando esta Activity foi aberta
    private val originalExistingItems = mutableListOf<ShoppingItem>()

    private lateinit var suggestedProductAdapter: SuggestedProductAdapter
    private lateinit var selectedProductAdapter: SelectedProductAdapter

    private lateinit var suggestedProductViewModel: SuggestedProductViewModel // NOVO: ViewModel para sugestões

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

        // NOVO: Inicializar SuggestedProductViewModel
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ShoppingListRepository(database.shoppingListDao(), database.shoppingItemDao(), database.suggestedProductDao())
        suggestedProductViewModel = ViewModelProvider(this, SuggestedProductViewModelFactory(repository))
            .get(SuggestedProductViewModel::class.java)

        // NOVO: Observar as sugestões do ViewModel
        suggestedProductViewModel.allSuggestedProducts.observe(this) { suggestedProducts : List<SuggestedProduct> ->
            allSuggestedProductsFromDb = suggestedProducts
            Log.d("AddItemActivity", "Observer de SuggestedProducts disparado. Sugestões recebidas: ${suggestedProducts.size}")

            // Popula sugestões iniciais APENAS se o banco estiver vazio.
            // É importante verificar AQUI, após o observer ter preenchido allSuggestedProductsFromDb.
            if (allSuggestedProductsFromDb.isEmpty()) {
                populateInitialSuggestedNamesIfDbEmpty()
            }

            filterSuggestedItems(binding.editTextSearchItem.text.toString()) // Re-filtra e atualiza a UI
        }


        setupSelectedItemsRecyclerView()
        setupSuggestedItemsRecyclerView()

        setupSearchInput()
        setupAddNewSuggestionFab()

        updateSelectedItemsUI() // Atualiza a UI de itens selecionados inicialmente
        // filterSuggestedItems já é chamado no observer do ViewModel quando as sugestões são carregadas.
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

                    // Se o item removido NÃO era um item original E seu nome não está nas sugestões atuais do banco,
                    // adicione-o de volta como uma nova sugestão. Isso lida com itens adicionados manualmente que
                    // não eram sugestões padrão ou já foram removidos anteriormente da seleção.
                    val isOriginalItem = originalExistingItems.any { it.id == itemToRemove.id }
                    val isBaseSuggestionAlready = allSuggestedProductsFromDb.any { it.name.equals(itemToRemove.name, ignoreCase = true) }

                    if (!isOriginalItem && !isBaseSuggestionAlready) {
                        // Converte o nome de volta para um SuggestedProduct e insere no DB via ViewModel
                        suggestedProductViewModel.insert(SuggestedProduct(name = itemToRemove.name))
                        Log.d("AddItemActivity", "DEBUG_CALLBACK: '${itemToRemove.name}' adicionado de volta às sugestões do DB.")
                    } else if (isOriginalItem) {
                        Log.d("AddItemActivity", "DEBUG_CALLBACK: '${itemToRemove.name}' era um item original. Não adicionado de volta às sugestões.")
                    }
                    updateSelectedItemsUI()
                    // filterSuggestedItems() será chamado automaticamente pelo observer do ViewModel.
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

    // NOVO MÉTODO: Diálogo de confirmação para deletar sugestão (agora recebe SuggestedProduct)
    private fun showDeleteSuggestionConfirmationDialog(suggestion: SuggestedProduct) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Excluir Sugestão?")
            .setMessage("Tem certeza que deseja remover a sugestão '${suggestion.name}' da sua lista de sugestões base? Isso não afetará os itens já adicionados às listas de compras.")
            .setPositiveButton("Excluir") { dialog, _ ->
                suggestedProductViewModel.delete(suggestion) // Deleta via ViewModel
                Toast.makeText(this, "Sugestão '${suggestion.name}' removida.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // REMOVEMOS O MÉTODO 'deleteSuggestion(String)' POIS A EXCLUSÃO AGORA É FEITA VIA VIEWMARX

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

                // A lista de sugestões filtradas será atualizada automaticamente via o observer do ViewModel
                // e o filterSuggestedItems já considera os itens selecionados, então não precisa remover daqui.
                updateSelectedItemsUI()
                filterSuggestedItems(binding.editTextSearchItem.text.toString())
            },
            onDeleteLongClick = { suggestionName -> // NOVO CALLBACK para clique longo
                // Encontre o objeto SuggestedProduct completo para deletar
                val suggestedProductToDelete = allSuggestedProductsFromDb.find { it.name == suggestionName }
                suggestedProductToDelete?.let {
                    showDeleteSuggestionConfirmationDialog(it) // Passa o objeto completo para o diálogo
                } ?: Log.e("AddItemActivity", "Sugestão '$suggestionName' não encontrada para exclusão (long click).")
                true // Indica que o evento foi consumido
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

                // Verifica se a sugestão já existe na lista de sugestões do banco de dados
                if (allSuggestedProductsFromDb.any { it.name.equals(newItemName, ignoreCase = true) }) {
                    Toast.makeText(this, "Sugestão '${newItemName}' já existe nas sugestões.", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    return@setPositiveButton
                }

                // Verifica se o item já está na lista de itens selecionados (incluindo os originais)
                if (selectedShoppingItems.any { it.name.equals(newItemName, ignoreCase = true) }) {
                    Toast.makeText(this, "Item '${newItemName}' já está na sua lista selecionada.", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                    return@setPositiveButton
                }

                // NOVO: Insere a nova sugestão no banco de dados via ViewModel
                val newSuggestedProduct = SuggestedProduct(name = newItemName)
                suggestedProductViewModel.insert(newSuggestedProduct)
                Log.d("AddItemActivity", "DEBUG_FAB: Nova sugestão '${newItemName}' adicionada ao DB e selecionada automaticamente.")

                // Cria o novo ShoppingItem e o adiciona à lista de selecionados
                val newItem = ShoppingItem(
                    id = UUID.randomUUID().toString(),
                    name = newItemName,
                    quantity = 1,
                    isPurchased = false,
                    listId = currentListId ?: "" // Usa o ID da lista real
                )
                selectedShoppingItems.add(newItem)

                Toast.makeText(this, "Sugestão '${newItemName}' adicionada e selecionada!", Toast.LENGTH_SHORT).show()

                // filterSuggestedItems será chamado automaticamente pelo observer quando o DB atualizar
                updateSelectedItemsUI()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // MODIFICADO: Populamos sugestões apenas se o banco de dados estiver vazio
    private fun populateInitialSuggestedNamesIfDbEmpty() {
        // Verifica allSuggestedProductsFromDb, que é preenchido pelo LiveData do ViewModel
        if (allSuggestedProductsFromDb.isEmpty()) {
            val initialSuggestions = listOf(
                "Alface", "Arroz", "Atum", "Açúcar", "Banana", "Batata",
                "Carne bovina", "Carne frango", "Cebola", "Cenoura", "Cereal",
                "Café", "Leite", "Pão", "Ovos", "Salsicha", "Tomate", "Macarrão",
                "Chocolate", "Refrigerante", "Feijão", "Abacaxi", "Cerveja"
                // Adicione mais sugestões conforme necessário
            ).map { SuggestedProduct(name = it) } // Mapeia para SuggestedProduct

            initialSuggestions.forEach { suggestedProductViewModel.insert(it) }
            Log.d("AddItemActivity", "DEBUG_POPULATE: Sugestões iniciais populadas no DB. Total: ${initialSuggestions.size}")
        } else {
            Log.d("AddItemActivity", "DEBUG_POPULATE: Banco de dados de sugestões já contém itens. Não populando iniciais.")
        }
    }


    private fun filterSuggestedItems(query: String) {
        filteredSuggestedNames.clear()
        val lowerCaseQuery = query.lowercase(Locale.ROOT)
        // Obtém os nomes dos itens que já estão na lista de selecionados (incluindo os originais)
        val currentSelectedNames = selectedShoppingItems.map { it.name.lowercase(Locale.ROOT) }.toSet()

        Log.d("AddItemActivity", "DEBUG_FILTER: Filtrando sugestões para query: '$query'. Itens selecionados atualmente (para exclusão da sugestão): ${currentSelectedNames.size}")

        // Agora filtra a partir de allSuggestedProductsFromDb
        for (suggestedProduct in allSuggestedProductsFromDb) {
            val itemLowerCase = suggestedProduct.name.lowercase(Locale.ROOT)
            if (itemLowerCase.contains(lowerCaseQuery) && !currentSelectedNames.contains(itemLowerCase)) {
                filteredSuggestedNames.add(suggestedProduct.name) // Adiciona APENAS o nome para o adapter
            }
        }
        suggestedProductAdapter.updateSuggestions(filteredSuggestedNames.sorted()) // Certifica-se de que a lista está ordenada
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