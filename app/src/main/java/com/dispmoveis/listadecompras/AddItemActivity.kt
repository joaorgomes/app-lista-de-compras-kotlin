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

    private lateinit var suggestedProductViewModel: SuggestedProductViewModel //ViewModel para sugestões

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
            originalExistingItems.addAll(it) // Salva os itens originais
            selectedShoppingItems.addAll(it.map { item -> item.copy() }) // Adiciona cópias para a lista de trabalho
        }

        // NOVO: Inicializar SuggestedProductViewModel
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ShoppingListRepository(database.shoppingListDao(), database.shoppingItemDao(), database.suggestedProductDao())
        suggestedProductViewModel = ViewModelProvider(this, SuggestedProductViewModelFactory(repository))
            .get(SuggestedProductViewModel::class.java)

        // NOVO: Observar as sugestões do ViewModel
        suggestedProductViewModel.allSuggestedProducts.observe(this) { suggestedProducts : List<SuggestedProduct> ->
            allSuggestedProductsFromDb = suggestedProducts
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

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_add_item, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_finish_add_items -> {
                onFinishAddingItems()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // lógica de finalizar e retornar os itens
    private fun onFinishAddingItems() {
        val resultIntent = Intent()
        // Coloca a lista de itens selecionados como um extra no Intent de resultado
        resultIntent.putParcelableArrayListExtra(EXTRA_SELECTED_SHOPPING_ITEMS_RESULT, ArrayList(selectedShoppingItems))
        // Define o resultado da Activity como OK e anexa o Intent
        setResult(Activity.RESULT_OK, resultIntent)
        // Finaliza AddItemActivity, retornando para a Activity anterior(ShoppingListDetailActivity)
        finish()
    }

    //Sobrescreve o métodoonBackPressed para garantir que o retorno seja feito
    override fun onBackPressed() {
        onFinishAddingItems()
        //Super.onBackPressed() //é chamado implicitamente por finish()
    }

    private fun setupSelectedItemsRecyclerView() {
        selectedProductAdapter = SelectedProductAdapter(
            onQuantityChange = { item ->
                updateSelectedItemsUI()
            },
            onRemoveClick = { itemToRemove ->
                val removed = selectedShoppingItems.remove(itemToRemove) // Remove a instância correta

                if (removed) {
                    // Se o item removido NÃO era um item original E seu nome não está nas sugestões atuais do banco,
                    // adicione-o de volta como uma nova sugestão. Isso lida com itens adicionados manualmente que
                    // não eram sugestões padrão ou já foram removidos anteriormente da seleção.
                    val isOriginalItem = originalExistingItems.any { it.id == itemToRemove.id }
                    val isBaseSuggestionAlready = allSuggestedProductsFromDb.any { it.name.equals(itemToRemove.name, ignoreCase = true) }

                    if (!isOriginalItem && !isBaseSuggestionAlready) {
                        // Converte o nome de volta para um SuggestedProduct e insere no DB via ViewModel
                        suggestedProductViewModel.insert(SuggestedProduct(name = itemToRemove.name))
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
    }

    // Diálogo de confirmação para deletar sugestão (agora recebe SuggestedProduct)
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

    private fun setupSuggestedItemsRecyclerView() {
        suggestedProductAdapter = SuggestedProductAdapter(
            filteredSuggestedNames,
            onAddClick = { productName ->


                val existingItemInSelection = selectedShoppingItems.find { it.name.equals(productName, ignoreCase = true) }

                if (existingItemInSelection != null) {
                    existingItemInSelection.quantity++
                    selectedProductAdapter.notifyItemChanged(selectedShoppingItems.indexOf(existingItemInSelection))
                } else {
                    // Cria um NOVO ShoppingItem com um ID único e o currentListId
                    val newItem = ShoppingItem(
                        id = UUID.randomUUID().toString(),
                        name = productName,
                        quantity = 1,
                        isPurchased = false,
                        listId = currentListId ?: "" // Usa o ID da lista real
                    )
                    selectedShoppingItems.add(newItem)
                }
                // A lista de sugestões filtradas será atualizada automaticamente via o observer do ViewModel
                // e o filterSuggestedItems já considera os itens selecionados, então não precisa remover daqui.
                updateSelectedItemsUI()
                filterSuggestedItems(binding.editTextSearchItem.text.toString())
            },
            onDeleteLongClick = { suggestionName -> // NOVO CALLBACK para clique longo
                // Encontra o objeto SuggestedProduct completo para deletar
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

                //Insere a nova sugestão no banco de dados via ViewModel
                val newSuggestedProduct = SuggestedProduct(name = newItemName)
                suggestedProductViewModel.insert(newSuggestedProduct)

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

    //Popula sugestões apenas se o banco de dados estiver vazio
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
        } else {
            Log.d("AddItemActivity", "DEBUG_POPULATE: Banco de dados de sugestões já contém itens. Não populando iniciais.")
        }
    }


    private fun filterSuggestedItems(query: String) {
        filteredSuggestedNames.clear()
        val lowerCaseQuery = query.lowercase(Locale.ROOT)
        // Obtém os nomes dos itens que já estão na lista de selecionados (incluindo os originais)
        val currentSelectedNames = selectedShoppingItems.map { it.name.lowercase(Locale.ROOT) }.toSet()

        // filtra a partir de allSuggestedProductsFromDb
        for (suggestedProduct in allSuggestedProductsFromDb) {
            val itemLowerCase = suggestedProduct.name.lowercase(Locale.ROOT)
            if (itemLowerCase.contains(lowerCaseQuery) && !currentSelectedNames.contains(itemLowerCase)) {
                filteredSuggestedNames.add(suggestedProduct.name) // Adiciona APENAS o nome para o adapter
            }
        }
        suggestedProductAdapter.updateSuggestions(filteredSuggestedNames.sorted()) // Certifica-se de que a lista está ordenada
    }

    private fun updateSelectedItemsUI() {

        if (selectedShoppingItems.isEmpty()) {
            binding.textViewSelectedItemsTitle.visibility = View.GONE
            binding.recyclerViewSelectedItems.visibility = View.GONE
        } else {
            binding.textViewSelectedItemsTitle.visibility = View.VISIBLE
            binding.recyclerViewSelectedItems.visibility = View.VISIBLE

        }
        selectedProductAdapter.updateList(selectedShoppingItems.sortedBy { it.name }) // Ordena para melhor visualização
    }
}