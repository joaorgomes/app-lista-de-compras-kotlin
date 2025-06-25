package com.dispmoveis.listadecompras

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.dispmoveis.listadecompras.databinding.ActivityShoppingListDetailBinding
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts // <<-- NOVO IMPORT
import android.app.Activity // <<-- NOVO IMPORT

class ShoppingListDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShoppingListDetailBinding
    private var listName: String? = null

    private val shoppingListItems = mutableListOf<ShoppingItem>()
    private lateinit var shoppingListItemAdapter: ShoppingListItemAdapter

    // NOVO: Launcher para AddItemActivity
    private val addItemsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val newProducts = data?.getParcelableArrayListExtra<ProductItem>("SELECTED_PRODUCTS")

            newProducts?.let {
                for (productItem in it) {
                    // Converte ProductItem para ShoppingItem e adiciona à lista
                    val shoppingItem = ShoppingItem(productItem.name, false)
                    // Você pode precisar de uma lógica mais sofisticada se quiser
                    // usar 'productItem.quantity' ou 'productItem.customQuantityText' aqui.
                    // Por simplicidade, vamos usar apenas o nome por enquanto,
                    // mas idealmente ShoppingItem também deveria ter quantidade.
                    // Por agora, o ShoppingItem é apenas o nome e o estado de compra.
                    // Se você quer a quantidade aparecendo na lista principal,
                    // você precisaria expandir o data class ShoppingItem.
                    shoppingListItemAdapter.addItem(shoppingItem)
                }
                updateUIBasedOnItems()
            }
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

        shoppingListItemAdapter = ShoppingListItemAdapter(
            shoppingListItems,
            onItemClick = { clickedItem ->
                clickedItem.isPurchased = !clickedItem.isPurchased
                shoppingListItemAdapter.notifyItemChanged(shoppingListItems.indexOf(clickedItem))
                Toast.makeText(this, "Estado de '${clickedItem.name}' alterado!", Toast.LENGTH_SHORT).show()
            },
            onEditClick = { editedItem ->
                Toast.makeText(this, "Editar item '${editedItem.name}'", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { deletedItem ->
                shoppingListItemAdapter.removeItem(deletedItem)
                updateUIBasedOnItems()
                Toast.makeText(this, "Deletado: ${deletedItem.name}", Toast.LENGTH_SHORT).show()
            }
        )

        binding.recyclerViewListItems.apply {
            layoutManager = LinearLayoutManager(this@ShoppingListDetailActivity)
            adapter = shoppingListItemAdapter
        }

        updateUIBasedOnItems()

        binding.fabAddItem.setOnClickListener {
            val intent = Intent(this, AddItemActivity::class.java)
            addItemsLauncher.launch(intent) // Usa o launcher para iniciar a Activity e esperar o resultado
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateUIBasedOnItems() {
        if (shoppingListItems.isEmpty()) {
            binding.imageViewEmptyItems.visibility = View.VISIBLE
            binding.textViewEmptyItemsTitle.visibility = View.VISIBLE
            binding.textViewEmptyItemsSubtitle.visibility = View.VISIBLE
            binding.recyclerViewListItems.visibility = View.GONE
        } else {
            binding.imageViewEmptyItems.visibility = View.GONE
            binding.textViewEmptyItemsTitle.visibility = View.GONE
            binding.textViewEmptyItemsSubtitle.visibility = View.GONE
            binding.recyclerViewListItems.visibility = View.VISIBLE
        }
    }
}