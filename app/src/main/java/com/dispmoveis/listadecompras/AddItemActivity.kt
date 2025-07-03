package com.dispmoveis.listadecompras

import android.app.Activity
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
import android.util.Log

class AddItemActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddItemBinding

    private val allSuggestedNames = mutableListOf<String>()
    private val filteredSuggestedNames = mutableListOf<String>()

    // AGORA É UMA LISTA DE ShoppingItem
    private val selectedShoppingItems = mutableListOf<ShoppingItem>()
    private val originalExistingItems = mutableListOf<ShoppingItem>() // Para guardar a lista original

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

        setSupportActionBar(binding.toolbarAddItem)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.add_new_item_title)
        }
        binding.toolbarAddItem.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Receber itens existentes e inicializar selectedShoppingItems (AGORA ESPERA ShoppingItem)
        val existingItemsFromDetail = intent.getParcelableArrayListExtra<ShoppingItem>("EXISTING_SHOPPING_ITEMS")
        existingItemsFromDetail?.let {
            Log.d("AddItemActivity", "DEBUG_ONCREATE: Itens recebidos para inicialização: ${it.size}")
            selectedShoppingItems.addAll(it)
            // CLONAR A LISTA ORIGINAL PARA COMPARAÇÃO FUTURA!
            originalExistingItems.addAll(it.map { item -> item.copy() }) // Criar cópias para não modificar a original
            Log.d("AddItemActivity", "DEBUG_ONCREATE: selectedShoppingItems após adicionar existentes: ${selectedShoppingItems.size}")
            Log.d("AddItemActivity", "DEBUG_ONCREATE: originalExistingItems populado com ${originalExistingItems.size} itens.")
        }


        populateSuggestedItems()

        setupSelectedItemsRecyclerView()
        setupSuggestedItemsRecyclerView()

        setupSearchInput()
        setupAddButton()

        // Chamar updateUI inicialmente para configurar a visibilidade
        updateSelectedItemsUI()
        filterSuggestedItems(binding.editTextSearchItem.text.toString())
    }
    //MÉTODO para determinar o que foi alterado/adicionado
    private fun getChangedOrNewItems(): ArrayList<ShoppingItem> {
        val resultList = ArrayList<ShoppingItem>()

        // Iterar sobre os itens que o usuário 'selecionou' ou modificou
        selectedShoppingItems.forEach { currentItem ->
            val originalItem = originalExistingItems.find { it.id == currentItem.id }

            if (originalItem == null) {
                // Este é um item completamente NOVO, que não existia na lista original
                resultList.add(currentItem)
                Log.d("AddItemActivity", "DEBUG_RETURN: Item NOVO detectado: '${currentItem.name}', Qtd: ${currentItem.quantity}")
            } else {
                // É um item existente. Verificar se a quantidade ou outros atributos mudaram.
                // Para seu caso, o foco principal é a quantidade.
                if (currentItem.quantity != originalItem.quantity) {
                    // A quantidade de um item existente mudou
                    resultList.add(currentItem) // Retorna o item com a nova quantidade
                    Log.d("AddItemActivity", "DEBUG_RETURN: Item EXISTENTE com Qtd alterada: '${currentItem.name}', Qtd original: ${originalItem.quantity}, Qtd nova: ${currentItem.quantity}")
                }
                // Se isPurchased puder ser alterado aqui, você também precisaria verificar
                // if (currentItem.isPurchased != originalItem.isPurchased) { ... }
            }
        }
        return resultList
    }

    private fun setupSelectedItemsRecyclerView() {
        selectedProductAdapter = SelectedProductAdapter(
            //selectedShoppingItems, // Passa a lista REAL da Activity
            onQuantityChange = { item -> // Callback agora recebe ShoppingItem
                Log.d("AddItemActivity", "DEBUG_CALLBACK: Qtd '${item.name}' alterada para ${item.quantity}. selectedShoppingItems size: ${selectedShoppingItems.size}")
                updateSelectedItemsUI() // Chamar para atualizar a visibilidade
            },
            onRemoveClick = { itemToRemove -> // Callback agora recebe ShoppingItem
                Log.d("AddItemActivity", "DEBUG_CALLBACK: Tentando remover '${itemToRemove.name}'. selectedShoppingItems size ANTES: ${selectedShoppingItems.size}")
                val removed = selectedShoppingItems.remove(itemToRemove) // Remove diretamente da lista da Activity
                if (removed) {
                    Log.d("AddItemActivity", "DEBUG_CALLBACK: Item '${itemToRemove.name}' REMOVIDO. selectedShoppingItems size DEPOIS: ${selectedShoppingItems.size}")

                    // Adiciona o nome do item de volta para as sugestões disponíveis
                    if (!allSuggestedNames.contains(itemToRemove.name) && !filteredSuggestedNames.contains(itemToRemove.name)) {
                        allSuggestedNames.add(itemToRemove.name)
                        allSuggestedNames.sort()
                        Log.d("AddItemActivity", "DEBUG_CALLBACK: '${itemToRemove.name}' adicionado de volta às sugestões base.")
                    }

                    updateSelectedItemsUI() // Chamar para atualizar a visibilidade e o adapter
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

                val existingItem = selectedShoppingItems.find { it.name == productName }

                if (existingItem != null) {
                    existingItem.quantity++
                    Log.d("AddItemActivity", "DEBUG_ONADD: Incrementada quantidade de item existente: ${productName}, Qtd: ${existingItem.quantity}")
                } else {
                    // AGORA CRIA UM ShoppingItem
                    val newItem = ShoppingItem(name = productName, quantity = 1, price = 0.0)
                    selectedShoppingItems.add(newItem) // Adiciona DIRETAMENTE à lista da Activity
                    Log.d("AddItemActivity", "DEBUG_ONADD: Adicionado novo item sugerido: ${productName}, Qtd: ${newItem.quantity}.")
                }

                Log.d("AddItemActivity", "DEBUG_ONADD: selectedShoppingItems size DEPOIS da adição/incremento: ${selectedShoppingItems.size}")

                // Remove o nome da lista de sugestões (pois já foi adicionado/selecionado)
                allSuggestedNames.remove(productName)
                Log.d("AddItemActivity", "DEBUG_ONADD: '$productName' removido de allSuggestedNames. AllSuggestedNames size: ${allSuggestedNames.size}")

                filterSuggestedItems(binding.editTextSearchItem.text.toString())
                updateSelectedItemsUI() // Chamar para atualizar a visibilidade e o adapter
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

    private fun setupAddButton() {
        binding.buttonFinishAddingItems.setOnClickListener {
            val resultIntent = Intent()
            // Retorna a lista de ShoppingItem
            // Retorna APENAS os itens que foram adicionados ou tiveram a quantidade alterada
            val itemsToReturn = getChangedOrNewItems()
            Log.d("AddItemActivity", "DEBUG_FINISH: Retornando ${itemsToReturn.size} itens (novos/modificados).")
            itemsToReturn.forEachIndexed { index, item ->
                Log.d("AddItemActivity", "DEBUG_FINISH: Item a retornar[$index]: Name='${item.name}', Qtd=${item.quantity}, Price=${item.price}, Purchased=${item.isPurchased}, ID=${item.id}")
            }
            resultIntent.putParcelableArrayListExtra("SELECTED_SHOPPING_ITEMS_RESULT", ArrayList(selectedShoppingItems))
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
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
        val lowerCaseQuery = query.lowercase()
        val currentSelectedNames = selectedShoppingItems.map { it.name.lowercase() }.toSet() // Usa ShoppingItem.name

        Log.d("AddItemActivity", "DEBUG_FILTER: Filtrando sugestões para query: '$query'. Itens selecionados atualmente para exclusão: ${currentSelectedNames.size}")

        for (item in allSuggestedNames) {
            val itemLowerCase = item.lowercase()
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
        // ESSENCIAL: Sincroniza o adapter com a lista REAL DA ACTIVITY
        selectedProductAdapter.updateList(selectedShoppingItems) // <<-- NOVA CHAMADA CRÍTICA AQUI
        Log.d("AddItemActivity", "DEBUG_UI: Chamado selectedProductAdapter.updateList com ${selectedShoppingItems.size} itens (depois da chamada ao adapter).")
    }

}