package com.dispmoveis.listadecompras

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.dispmoveis.listadecompras.adapters.ShoppingListItemAdapter
import com.dispmoveis.listadecompras.database.AppDatabase
import com.dispmoveis.listadecompras.databinding.ActivityShoppingListDetailBinding
import com.dispmoveis.listadecompras.model.ShoppingItem
import com.dispmoveis.listadecompras.repository.ShoppingListRepository
import com.dispmoveis.listadecompras.viewmodel.ShoppingItemViewModel
import com.dispmoveis.listadecompras.viewmodel.ShoppingItemViewModelFactory
import com.dispmoveis.listadecompras.viewmodel.ShoppingListViewModel
import com.dispmoveis.listadecompras.viewmodel.ShoppingListViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.text.NumberFormat
import java.util.Locale

class ShoppingListDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShoppingListDetailBinding
    private var listName: String? = null

    private lateinit var shoppingListItemAdapter: ShoppingListItemAdapter
    private lateinit var shoppingItemViewModel: ShoppingItemViewModel
    private lateinit var shoppingListViewModel: ShoppingListViewModel

    private lateinit var currentListId: String

    // Adicionado para manter uma cópia dos itens atuais vindos do banco de dados (via LiveData)
    private var currentItemsInDb: List<ShoppingItem> = emptyList()

    companion object {
        const val EXTRA_LIST_ID = "LIST_ID"
        const val EXTRA_LIST_NAME = "LIST_NAME"
    }

    // Launcher para AddItemActivity
    private val addItemsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val finalSelectedItems = data?.getParcelableArrayListExtra<ShoppingItem>(AddItemActivity.EXTRA_SELECTED_SHOPPING_ITEMS_RESULT)

            finalSelectedItems?.let { returnedItems ->
                Log.d("ShoppingListDetail", "Itens retornados da AddItemActivity: ${returnedItems.size}")

                // Lógica de conciliação:
                val itemsToAdd = mutableListOf<ShoppingItem>()
                val itemsToUpdate = mutableListOf<ShoppingItem>()
                val itemsToDelete = mutableListOf<ShoppingItem>()

                // 1. Identificar itens a serem adicionados ou atualizados
                for (returnedItem in returnedItems) {
                    val existingItem = currentItemsInDb.find { it.id == returnedItem.id }
                    if (existingItem == null) {
                        // Item completamente novo (ID não encontrado no banco de dados atual)
                        itemsToAdd.add(returnedItem.copy(listId = currentListId)) // Garante o listId correto
                    } else if (existingItem != returnedItem) { // Verifica se algo mudou (usa data class equals)
                        // Item existente com propriedades modificadas
                        itemsToUpdate.add(returnedItem.copy(listId = currentListId)) // Garante o listId e atualiza
                    }
                }

                // 2. Identificar itens a serem removidos
                for (dbItem in currentItemsInDb) {
                    if (returnedItems.none { it.id == dbItem.id }) {
                        // Item que estava no banco de dados mas não está mais na lista retornada
                        itemsToDelete.add(dbItem)
                    }
                }

                // Executar as operações no ViewModel/Repository
                if (itemsToAdd.isNotEmpty()) {
                    shoppingItemViewModel.insertItems(itemsToAdd)
                    Snackbar.make(binding.root, "${itemsToAdd.size} novos itens adicionados!", Snackbar.LENGTH_SHORT).show()
                    Log.d("ShoppingListDetail", "Adicionados: ${itemsToAdd.map { it.name }}")
                }
                if (itemsToUpdate.isNotEmpty()) {
                    itemsToUpdate.forEach { item -> shoppingItemViewModel.update(item) }
                    Snackbar.make(binding.root, "${itemsToUpdate.size} itens atualizados!", Snackbar.LENGTH_SHORT).show()
                    Log.d("ShoppingListDetail", "Atualizados: ${itemsToUpdate.map { it.name }}")
                }
                if (itemsToDelete.isNotEmpty()) {
                        itemsToDelete.forEach { item -> shoppingItemViewModel.delete(item) } // OU shoppingItemViewModel.deleteItems(itemsToDelete) se tiver um método em massa
                        Snackbar.make(binding.root, "${itemsToDelete.size} itens removidos!", Snackbar.LENGTH_SHORT).show()
                         Log.d("ShoppingListDetail", "Removidos: ${itemsToDelete.map { it.name }}")
                     }
                if (itemsToDelete.isNotEmpty()) {
                    shoppingItemViewModel.deleteItems(itemsToDelete) // Chame o novo método de exclusão em massa
                    Snackbar.make(binding.root, "${itemsToDelete.size} itens removidos!", Snackbar.LENGTH_SHORT).show()
                    Log.d("ShoppingListDetail", "Removidos: ${itemsToDelete.map { it.name }}")
                }


                if (itemsToAdd.isEmpty() && itemsToUpdate.isEmpty() && itemsToDelete.isEmpty()) {
                    Snackbar.make(binding.root, "Nenhuma alteração na lista de itens.", Snackbar.LENGTH_SHORT).show()
                }

            } ?: run {
                Log.d("ShoppingListDetail", "Nenhum item selecionado ou erro ao receber dados da AddItemActivity.")
            }
        } else {
            Log.d("ShoppingListDetail", "Seleção de itens na AddItemActivity cancelada ou falhou.")
        }
    }

    // Launcher para EditItemActivity (para edição de UM ÚNICO item)
    private val editItemLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val editedItem = data?.getParcelableExtra<ShoppingItem>(EditItemActivity.EXTRA_EDITED_SHOPPING_ITEM_RESULT)

            editedItem?.let { updatedItem ->
                Log.d("ShoppingListDetail", "Item editado recebido da EditItemActivity: '${updatedItem.name}', Qtd=${updatedItem.quantity}, Price=${updatedItem.price}, Purchased=${updatedItem.isPurchased}")
                shoppingItemViewModel.update(updatedItem) // Atualiza via ViewModel
                Toast.makeText(this, "Item '${updatedItem.name}' editado com sucesso!", Toast.LENGTH_SHORT).show()
            } ?: run {
                Log.d("ShoppingListDetail", "Nenhum item editado recebido da EditItemActivity.")
            }
        } else {
            Log.d("ShoppingListDetail", "Edição de item na EditItemActivity cancelada ou falhou.")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityShoppingListDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        currentListId = intent.getStringExtra(EXTRA_LIST_ID) ?: run {
            Toast.makeText(this, "Erro: ID da lista não fornecido.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        listName = intent.getStringExtra(EXTRA_LIST_NAME)

        setSupportActionBar(binding.toolbarDetail)
        supportActionBar?.apply {
            title = listName ?: "Detalhes da Lista"
            setDisplayHomeAsUpEnabled(true)
        }

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ShoppingListRepository(database.shoppingListDao(), database.shoppingItemDao(),database.suggestedProductDao())

        shoppingItemViewModel = ViewModelProvider(this, ShoppingItemViewModelFactory(repository))
            .get(ShoppingItemViewModel::class.java)

        shoppingListViewModel = ViewModelProvider(this, ShoppingListViewModelFactory(repository))
            .get(ShoppingListViewModel::class.java)

        shoppingItemViewModel.setCurrentListId(currentListId)

        shoppingListItemAdapter = ShoppingListItemAdapter(
            onItemClick = { clickedItem ->
                val updatedItem = clickedItem.copy(isPurchased = !clickedItem.isPurchased)
                shoppingItemViewModel.update(updatedItem)
                Toast.makeText(this, "Estado de '${clickedItem.name}' alterado!", Toast.LENGTH_SHORT).show()
            },
            onEditClick = { itemToEdit ->
                val intent = Intent(this, EditItemActivity::class.java).apply {
                    putExtra("SHOPPING_ITEM_TO_EDIT", itemToEdit)
                }
                editItemLauncher.launch(intent)
            },
            onDeleteClick = { itemToDelete ->
                showDeleteItemConfirmationDialog(itemToDelete)
            }
        )

        binding.recyclerViewListItems.apply {
            layoutManager = LinearLayoutManager(this@ShoppingListDetailActivity)
            adapter = shoppingListItemAdapter
        }

        shoppingItemViewModel.itemsForCurrentList.observe(this) { items ->
            Log.d("ShoppingListDetail", "Observer de itemsForCurrentList disparado. Itens recebidos: ${items.size}")
            currentItemsInDb = items // <-- MUITO IMPORTANTE: Atualiza a lista de itens do banco
            val filteredItems = applyFilter(items, getCurrentFilterType())
            shoppingListItemAdapter.updateItems(filteredItems)
            updateUIBasedOnItems(items.isEmpty())
            updateTotalValue(items)
        }



        binding.fabAddItem.setOnClickListener {
            val intent = Intent(this, AddItemActivity::class.java).apply {
                // Passa o ID da lista para a AddItemActivity
                putExtra(AddItemActivity.EXTRA_CURRENT_LIST_ID, currentListId)
                // Passa os itens ATUAIS da lista para a AddItemActivity (para filtro de sugestões)
                putParcelableArrayListExtra(AddItemActivity.EXTRA_EXISTING_SHOPPING_ITEMS, ArrayList(currentItemsInDb))
            }
            addItemsLauncher.launch(intent)
        }

        setupFilterChips()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }


    private fun showAddItemManuallyDialog() {
        val input = EditText(this)
        input.hint = "Nome do novo item"

        AlertDialog.Builder(this)
            .setTitle("Adicionar Item")
            .setView(input)
            .setPositiveButton("Adicionar") { dialog, _ ->
                val itemName = input.text.toString().trim()
                if (itemName.isNotEmpty()) {
                    // Verifica se o item já existe na lista atual (pelo nome, ignorando caixa)
                    if (currentItemsInDb.any { it.name.equals(itemName, ignoreCase = true) }) {
                        Toast.makeText(this, "Item '$itemName' já existe nesta lista.", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    val newItem = ShoppingItem(name = itemName, listId = currentListId)
                    shoppingItemViewModel.insert(newItem)
                    Toast.makeText(this, "Item '$itemName' adicionado.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "O nome do item não pode estar vazio.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteItemConfirmationDialog(item: ShoppingItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Excluir Item?")
            .setMessage("Tem certeza que deseja excluir o item '${item.name}'?")
            .setPositiveButton("Excluir") { dialog, _ ->
                shoppingItemViewModel.delete(item)
                Toast.makeText(this, "Item '${item.name}' excluído.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }



    private fun updateUIBasedOnItems(isEmpty: Boolean) {
        if (isEmpty) {
            binding.imageViewEmptyItems.visibility = View.VISIBLE
            binding.textViewEmptyItemsTitle.visibility = View.VISIBLE
            binding.textViewEmptyItemsSubtitle.visibility = View.VISIBLE
            binding.recyclerViewListItems.visibility = View.GONE
            Log.d("ShoppingListDetail", "Lista de itens vazia. Mostrando placeholder.")
        } else {
            binding.imageViewEmptyItems.visibility = View.GONE
            binding.textViewEmptyItemsTitle.visibility = View.GONE
            binding.textViewEmptyItemsSubtitle.visibility = View.GONE
            binding.recyclerViewListItems.visibility = View.VISIBLE
            Log.d("ShoppingListDetail", "Itens existentes. Mostrando RecyclerView.")
        }
    }

    @SuppressLint("StringFormatInvalid")
    private fun updateTotalValue(items: List<ShoppingItem>) {
        Log.d("ShoppingListDetail", "Iniciando cálculo do valor total dos itens comprados.")

        val total = items
            .filter { it.isPurchased }
            .sumOf { it.quantity * it.price }

        val formattedTotal = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(total)

        binding.totalValueTextView.text = getString(R.string.total_value_placeholder, formattedTotal)
        Log.d("ShoppingListDetail", "TextView atualizado para: ${binding.totalValueTextView.text}")

        Log.d("ShoppingListDetail", "Valor total dos comprados calculado: $formattedTotal")
    }

    private var currentFilterType: FilterType = FilterType.ALL

    private fun setupFilterChips() {
        binding.chipAll.setOnClickListener {
            filterAndApplyList(FilterType.ALL)
            currentFilterType = FilterType.ALL
        }
        binding.chipMissing.setOnClickListener {
            filterAndApplyList(FilterType.MISSING)
            currentFilterType = FilterType.MISSING
        }
        binding.chipPurchased.setOnClickListener {
            filterAndApplyList(FilterType.PURCHASED)
            currentFilterType = FilterType.PURCHASED
        }
        binding.chipAll.isChecked = true
    }

    private fun filterAndApplyList(filterType: FilterType) {
        shoppingItemViewModel.itemsForCurrentList.value?.let { allItems ->
            val filteredItems = applyFilter(allItems, filterType)
            shoppingListItemAdapter.updateItems(filteredItems)
            Log.d("ShoppingListDetail", "Lista filtrada ($filterType): ${filteredItems.size} itens. Adapter agora tem: ${shoppingListItemAdapter.itemCount}")
        }
    }

    private fun applyFilter(items: List<ShoppingItem>, filterType: FilterType): List<ShoppingItem> {
        return when (filterType) {
            FilterType.ALL -> items
            FilterType.MISSING -> items.filter { !it.isPurchased }
            FilterType.PURCHASED -> items.filter { it.isPurchased }
        }
    }

    private fun getCurrentFilterType(): FilterType {
        return when {
            binding.chipAll.isChecked -> FilterType.ALL
            binding.chipMissing.isChecked -> FilterType.MISSING
            binding.chipPurchased.isChecked -> FilterType.PURCHASED
            else -> FilterType.ALL
        }
    }
}