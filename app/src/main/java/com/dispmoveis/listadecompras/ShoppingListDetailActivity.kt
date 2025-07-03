// Arquivo: ShoppingListDetailActivity.kt

package com.dispmoveis.listadecompras

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.dispmoveis.listadecompras.databinding.ActivityShoppingListDetailBinding
import java.text.NumberFormat
import java.util.Locale

class ShoppingListDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShoppingListDetailBinding
    private var listName: String? = null

    // Lista de ShoppingItem
    private val shoppingListItems = mutableListOf<ShoppingItem>() // <<-- Esta é a lista principal

    private lateinit var shoppingListItemAdapter: ShoppingListItemAdapter

    // Launcher para AddItemActivity
    private val addItemsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val changedOrNewShoppingItems = data?.getParcelableArrayListExtra<ShoppingItem>("SELECTED_SHOPPING_ITEMS_RESULT")

            changedOrNewShoppingItems?.let {
                Log.d("ShoppingListDetail", "DEBUG: Itens recebidos (novos/modificados) da AddItemActivity: ${it.size}")
                Log.d("ShoppingListDetail", "DEBUG: shoppingListItems (ANTES do merge na Detail): ${shoppingListItems.size}")

                it.forEach { changedItem ->
                    // Tenta encontrar o item na lista principal pelo ID (se ele já tiver um ID)
                    // Ou pelo nome, se for um item novo que ainda não tem um ID do banco de dados.
                    val existingInMainList = shoppingListItems.find { item -> item.id == changedItem.id }

                    if (existingInMainList != null) {
                        // Item existente (com ID). Atualiza todas as suas propriedades.
                        // Isso é importante porque a quantidade pode ter mudado.
                        existingInMainList.quantity = changedItem.quantity
                        existingInMainList.customQuantityText = changedItem.customQuantityText
                        existingInMainList.price = changedItem.price
                        existingInMainList.isPurchased = changedItem.isPurchased
                        Log.d("ShoppingListDetail", "DEBUG_MERGE_DETAIL: Item existente '${existingInMainList.name}' atualizado. Nova Qtd: ${existingInMainList.quantity}")
                    } else {
                        // É um item NOVO que não estava na lista original da DetailActivity
                        // Ou um item existente que não foi identificado pelo ID (menos provável se IDs são únicos)
                        shoppingListItems.add(changedItem)
                        Log.d("ShoppingListDetail", "DEBUG_MERGE_DETAIL: Adicionando item completamente novo: '${changedItem.name}' Qtd: ${changedItem.quantity}")
                    }
                }
                Log.d("ShoppingListDetail", "DEBUG: shoppingListItems (DEPOIS do merge na Detail): ${shoppingListItems.size}")

                // ESSENCIAL: Notifica o adapter COM A LISTA ATUALIZADA
                filterList(getCurrentFilterType()) // Re-filtra e atualiza o adapter
                updateUIBasedOnItems()
                updateTotalValue()

                Log.d("ShoppingListDetail", "DEBUG: Lista principal atualizada. Total de itens agora: ${shoppingListItems.size}")
            } ?: run {
                Log.d("ShoppingListDetail", "DEBUG: Nenhum item novo/modificado recebido da AddItemActivity (lista vazia ou nula).")
            }
        } else {
            Log.d("ShoppingListDetail", "DEBUG: AddItemActivity retornou RESULT_CANCELED ou falhou.")
        }
    }
    private val editItemLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val editedItem = data?.getParcelableExtra<ShoppingItem>("EDITED_SHOPPING_ITEM_RESULT")

            editedItem?.let { updatedItem ->
                Log.d("ShoppingListDetail", "DEBUG: Item editado recebido: '${updatedItem.name}', Qtd=${updatedItem.quantity}, Price=${updatedItem.price}, Purchased=${updatedItem.isPurchased}")

                // Encontre o item original na sua lista principal (shoppingListItems) pelo ID
                val index = shoppingListItems.indexOfFirst { it.id == updatedItem.id }

                if (index != -1) {
                    // Substitua o item antigo pelo item editado
                    shoppingListItems[index] = updatedItem
                    Log.d("ShoppingListDetail", "DEBUG_EDIT: Item '${updatedItem.name}' atualizado na lista principal.")

                    // Atualize o adapter e a UI
                    filterList(getCurrentFilterType()) // Re-aplica o filtro com o item atualizado
                    updateTotalValue()
                    Toast.makeText(this, "Item '${updatedItem.name}' editado com sucesso!", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("ShoppingListDetail", "DEBUG_EDIT: Item editado não encontrado na lista principal pelo ID: ${updatedItem.id}")
                    Toast.makeText(this, "Erro: Item editado não pôde ser encontrado na lista.", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Log.d("ShoppingListDetail", "DEBUG: Nenhum item editado recebido da EditItemActivity.")
            }
        } else {
            Log.d("ShoppingListDetail", "DEBUG: EditItemActivity retornou RESULT_CANCELED ou falhou.")
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

        setSupportActionBar(binding.toolbarDetail)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = ""
        }

        listName = intent.getStringExtra("LIST_NAME")
        listName?.let {
            supportActionBar?.title = it
        } ?: run {
            supportActionBar?.title = "Detalhes da Lista"
        }

        // Configuração do Adapter para ShoppingListItemAdapter (agora com ShoppingItem)
        shoppingListItemAdapter = ShoppingListItemAdapter(
            //shoppingListItems, // Passa a lista de ShoppingItem (é a mesma referência)
            onItemClick = { clickedItem ->
                Log.d("ShoppingListDetail", "DEBUG_CLICK: Item clicado ANTES de toggle: Name='${clickedItem.name}', Purchased=${clickedItem.isPurchased}")
                val itemToUpdate = shoppingListItems.find { it.id == clickedItem.id }
                if(itemToUpdate != null){

                    itemToUpdate.isPurchased = !itemToUpdate.isPurchased
                    Log.d("ShoppingListDetail", "DEBUG_CLICK: Item na lista principal DEPOIS de toggle: Name='${itemToUpdate.name}', Purchased=${itemToUpdate.isPurchased}")
                    filterList(getCurrentFilterType()) // Adicione esta linha se precisar re-filtrar
                    updateTotalValue()
                    Toast.makeText(this, "Estado de '${clickedItem.name}' alterado!", Toast.LENGTH_SHORT).show()
                }else{
                    Log.e("ShoppingListDetail", "DEBUG_CLICK: Item clicado não encontrado na lista principal pelo ID: ${clickedItem.id}")
                    Toast.makeText(this, "Erro: Item não encontrado para atualizar.", Toast.LENGTH_SHORT).show()
                }

                // Se você tiver filtros aplicados, re-aplique o filtro atual
            },
            onEditClick = { editedItem ->
                val intent = Intent(this, EditItemActivity::class.java).apply {
                    putExtra("SHOPPING_ITEM_TO_EDIT", editedItem)
                }
                editItemLauncher.launch(intent)
                //Toast.makeText(this, "Editar item '${editedItem.name}'", Toast.LENGTH_SHORT).show()
                // FUTURO: Abrir uma tela para editar o item (nome, quantidade, preço)
            },
            onDeleteClick = { deletedItem ->
                // Remove da lista principal e depois atualiza o adapter
                val removed = shoppingListItems.remove(deletedItem)
                if (removed) {
                    filterList(getCurrentFilterType()) // RE-APLICA O FILTRO APÓS REMOÇÃO para atualizar a exibição
                    updateUIBasedOnItems()
                    updateTotalValue()
                    Toast.makeText(this, "Deletado: ${deletedItem.name}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Erro: Item não encontrado para deletar.", Toast.LENGTH_SHORT).show()
                }
            }
        )

        binding.recyclerViewListItems.apply {
            layoutManager = LinearLayoutManager(this@ShoppingListDetailActivity)
            adapter = shoppingListItemAdapter
        }

        // Inicializa a UI e o valor total
        updateUIBasedOnItems()
        updateTotalValue()

        binding.fabAddItem.setOnClickListener {
            val intent = Intent(this, AddItemActivity::class.java)
            // Passa a lista atual de ShoppingItem para a AddItemActivity
            intent.putParcelableArrayListExtra("EXISTING_SHOPPING_ITEMS", ArrayList(shoppingListItems))
            addItemsLauncher.launch(intent) // Usa o launcher
        }

        setupFilterChips() // Configurar os chips de filtro
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateUIBasedOnItems() {
        if (shoppingListItems.isEmpty()) { // <<-- Sempre verifica esta lista
            binding.imageViewEmptyItems.visibility = View.VISIBLE
            binding.textViewEmptyItemsTitle.visibility = View.VISIBLE
            binding.textViewEmptyItemsSubtitle.visibility = View.VISIBLE
            binding.recyclerViewListItems.visibility = View.GONE
            Log.d("ShoppingListDetail", "DEBUG_UI: Lista principal está vazia. Mostrando placeholder.")
        } else {
            binding.imageViewEmptyItems.visibility = View.GONE
            binding.textViewEmptyItemsTitle.visibility = View.GONE
            binding.textViewEmptyItemsSubtitle.visibility = View.GONE
            binding.recyclerViewListItems.visibility = View.VISIBLE
            Log.d("ShoppingListDetail", "DEBUG_UI: Lista principal tem ${shoppingListItems.size} itens. Mostrando RecyclerView.")
        }
    }

    @SuppressLint("StringFormatInvalid")
    private fun updateTotalValue() {
        Log.d("ShoppingListDetail", "DEBUG_TOTAL: Iniciando cálculo do valor total dos itens comprados.")

        // Lógica para somar APENAS os itens marcados como comprados
        val total = shoppingListItems
            .filter { it.isPurchased } // Filtra apenas os itens COMPRADOS
            .sumOf { it.quantity * it.price }

        // Formatar o valor para moeda brasileira (Real)
        val formattedTotal = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(total)

        // Atualizar o TextView no layout usando o ID que você forneceu
        // Assumindo que você está usando View Binding para activity_shopping_list_detail.xml
        binding.totalValueTextView.text = getString(R.string.total_value_placeholder, formattedTotal)
        Log.d("ShoppingListDetail", "DEBUG_UI_UPDATE: TextView atualizado para: ${binding.totalValueTextView.text}")

        Log.d("ShoppingListDetail", "DEBUG_TOTAL: Valor total dos comprados calculado: $formattedTotal")
    }

    private var currentFilterType: FilterType = FilterType.ALL // Variável para rastrear o filtro atual

    private fun setupFilterChips() {
        binding.chipAll.setOnClickListener {
            filterList(FilterType.ALL)
            currentFilterType = FilterType.ALL
        }
        binding.chipMissing.setOnClickListener {
            filterList(FilterType.MISSING)
            currentFilterType = FilterType.MISSING
        }
        binding.chipPurchased.setOnClickListener {
            filterList(FilterType.PURCHASED)
            currentFilterType = FilterType.PURCHASED
        }
        // Seleciona "Todos" por padrão
        binding.chipAll.isChecked = true
        filterList(FilterType.ALL)
    }

    private fun filterList(filterType: FilterType) {
        val filteredItems = when (filterType) {
            FilterType.ALL -> shoppingListItems // <<-- FILTRA SEMPRE A LISTA ORIGINAL
            FilterType.MISSING -> shoppingListItems.filter { !it.isPurchased }
            FilterType.PURCHASED -> shoppingListItems.filter { it.isPurchased }
        }
        shoppingListItemAdapter.updateItems(filteredItems) // <<-- Envia uma CÓPIA mutável
        Log.d("ShoppingListDetail", "Lista filtrada ($filterType): ${filteredItems.size} itens. Adapter agora tem: ${shoppingListItemAdapter.itemCount}")
    }

    private fun getCurrentFilterType(): FilterType {
        return when {
            binding.chipAll.isChecked -> FilterType.ALL
            binding.chipMissing.isChecked -> FilterType.MISSING
            binding.chipPurchased.isChecked -> FilterType.PURCHASED
            else -> FilterType.ALL // Default case
        }
    }
}