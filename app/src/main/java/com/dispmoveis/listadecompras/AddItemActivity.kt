package com.dispmoveis.listadecompras

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.dispmoveis.listadecompras.databinding.ActivityAddItemBinding
import android.text.Editable // Import para o TextWatcher
import android.text.TextWatcher // Import para o TextWatcher
import android.content.Intent // Import para Intent

class AddItemActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddItemBinding

    // Listas de dados
    private val allSuggestedItems = mutableListOf<String>() // Todos os itens padrão
    private val filteredSuggestedItems = mutableListOf<String>() // Itens sugeridos filtrados pela pesquisa
    private val selectedItems = mutableListOf<ProductItem>() // Itens que o usuário selecionou

    // Adaptadores
    private lateinit var suggestedProductAdapter: SuggestedProductAdapter
    private lateinit var selectedProductAdapter: SelectedProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAddItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- Configuração da Toolbar ---
        setSupportActionBar(binding.toolbarAddItem)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.add_new_item_title)
        }
        binding.toolbarAddItem.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // --- Preencher com dados de teste para sugestões ---
        populateSuggestedItems()

        // --- Configuração do RecyclerView de Itens Sugeridos ---
        suggestedProductAdapter = SuggestedProductAdapter(
            filteredSuggestedItems, // Começa com todos os itens
            onAddClick = { productName ->
                // Lógica para adicionar o item sugerido aos selecionados
                selectedProductAdapter.addOrUpdateItem(ProductItem(productName, 1))
                updateSelectedItemsUI() // Atualiza a visibilidade e o título
            }
        )
        binding.recyclerViewSuggestedItems.apply {
            layoutManager = LinearLayoutManager(this@AddItemActivity)
            adapter = suggestedProductAdapter
        }

        // --- Configuração do RecyclerView de Itens Selecionados ---
        selectedProductAdapter = SelectedProductAdapter(
            selectedItems,
            onQuantityChange = { productItem, newQuantity ->
                // Aqui você pode adicionar lógica para persistência futura
                // Por enquanto, apenas o adaptador já está atualizando a si mesmo
                updateSelectedItemsUI()
            },
            onRemoveClick = { productItem ->
                // Lógica para remover item dos selecionados
                selectedProductAdapter.removeItem(productItem)
                updateSelectedItemsUI() // Atualiza a visibilidade e o título
            }
        )
        binding.recyclerViewSelectedItems.apply {
            layoutManager = LinearLayoutManager(this@AddItemActivity)
            adapter = selectedProductAdapter
        }

        // --- Lógica do Campo de Pesquisa ---
        binding.editTextSearchItem.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterSuggestedItems(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // --- Lógica do Botão "Adicionar" (Finalizar adição de itens) ---
        binding.buttonFinishAddingItems.setOnClickListener {
            val resultIntent = Intent()
            val selectedItemsList = ArrayList(selectedProductAdapter.getSelectedItems())
            // Passa a lista de ProductItem de volta para a Activity anterior
            resultIntent.putExtra("SELECTED_PRODUCTS", selectedItemsList)
            setResult(RESULT_OK, resultIntent)
            finish() // Fecha AddItemActivity
        }

        // --- Atualizar a UI inicial ---
        updateSelectedItemsUI()
    }

    // --- Método para popular itens sugeridos (dados de teste) ---
    private fun populateSuggestedItems() {
        allSuggestedItems.addAll(listOf(
            "Alface", "Arroz", "Atum", "Açúcar", "Banana", "Batata",
            "Carne bovina", "Carne frango", "Cebola", "Cenoura", "Cereal",
            "Café", "Leite", "Pão", "Ovos", "Salsicha", "Tomate", "Macarrão"
        ))
        // Inicialmente, as sugestões filtradas são todas as sugestões
        filteredSuggestedItems.addAll(allSuggestedItems)
    }

    // --- Método para filtrar itens sugeridos ---
    private fun filterSuggestedItems(query: String) {
        filteredSuggestedItems.clear()
        if (query.isBlank()) {
            filteredSuggestedItems.addAll(allSuggestedItems)
        } else {
            val lowerCaseQuery = query.toLowerCase()
            for (item in allSuggestedItems) {
                if (item.toLowerCase().contains(lowerCaseQuery)) {
                    filteredSuggestedItems.add(item)
                }
            }
        }
        suggestedProductAdapter.updateSuggestions(filteredSuggestedItems)
    }

    // --- Método para controlar a visibilidade dos itens selecionados ---
    private fun updateSelectedItemsUI() {
        if (selectedItems.isEmpty()) {
            binding.textViewSelectedItemsTitle.visibility = View.GONE
            binding.recyclerViewSelectedItems.visibility = View.GONE
        } else {
            binding.textViewSelectedItemsTitle.visibility = View.VISIBLE
            binding.recyclerViewSelectedItems.visibility = View.VISIBLE
        }
    }
}