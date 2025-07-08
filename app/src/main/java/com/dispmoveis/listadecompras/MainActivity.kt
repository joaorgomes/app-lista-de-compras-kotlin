package com.dispmoveis.listadecompras

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.dispmoveis.listadecompras.adapters.ShoppingListAdapter
import com.dispmoveis.listadecompras.database.AppDatabase
import com.dispmoveis.listadecompras.databinding.ActivityMainBinding
import com.dispmoveis.listadecompras.model.ShoppingList
import com.dispmoveis.listadecompras.repository.ShoppingListRepository
import com.dispmoveis.listadecompras.viewmodel.ShoppingListViewModel
import com.dispmoveis.listadecompras.viewmodel.ShoppingListViewModelFactory
import com.google.android.material.navigation.NavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.threeten.bp.LocalDate

class MainActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener,
    AddListBottomSheetFragment.AddListListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle

    private lateinit var shoppingListAdapter: ShoppingListAdapter
    private lateinit var shoppingListViewModel: ShoppingListViewModel

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.app_name)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)
        }

        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ShoppingListRepository(database.shoppingListDao(), database.shoppingItemDao(),database.suggestedProductDao())
        shoppingListViewModel = ViewModelProvider(this, ShoppingListViewModelFactory(repository))
            .get(ShoppingListViewModel::class.java)

        shoppingListAdapter = ShoppingListAdapter(
            emptyList(),
            onItemClick = { clickedList ->
                val intent = Intent(this, ShoppingListDetailActivity::class.java).apply {
                    putExtra("LIST_ID", clickedList.id)
                    putExtra("LIST_NAME", clickedList.name)
                }
                startActivity(intent)
            },
            onEditClick = { listToEdit ->
                Log.d(TAG, "Clicou em editar lista: ${listToEdit.name}, ID: ${listToEdit.id}")
                showEditListDialog(listToEdit)
            },
            onDeleteClick = { listToDelete ->
                Log.d(TAG, "Clicou em deletar lista: ${listToDelete.name}, ID: ${listToDelete.id}")
                showDeleteConfirmationDialog(listToDelete)
            }
        )

        binding.recyclerViewLists.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = shoppingListAdapter
        }

        shoppingListViewModel.allShoppingLists.observe(this) { shoppingListsFromDb ->
            Log.d(TAG, "Observer de allShoppingLists disparado. Listas recebidas: ${shoppingListsFromDb.size}")
            shoppingListAdapter.updateLists(shoppingListsFromDb)
            updateUIBasedOnLists(shoppingListsFromDb.isEmpty())
        }

        binding.fabAddList.setOnClickListener {
            val addListBottomSheet = AddListBottomSheetFragment()
            addListBottomSheet.setAddListListener(this)
            addListBottomSheet.show(supportFragmentManager, addListBottomSheet.tag)
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    // MODIFICADO: Não infla nenhum menu.
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Não infla nenhum menu de opções na toolbar por padrão,
        // pois a navegação da gaveta já é gerenciada pelo ActionBarDrawerToggle.
        return true
    }

    // MODIFICADO: Remove a lógica para R.id.action_archive_all.
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Deixa o ActionBarDrawerToggle lidar com o clique no ícone do hambúrguer.
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        // Se houver outros itens de menu que você adicionar no futuro, você os trataria aqui.
        // Por enquanto, apenas repassa para a superclasse.
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_rate_us -> Toast.makeText(this, "Avalie-nos clicado!", Toast.LENGTH_SHORT).show()
            R.id.nav_share -> Toast.makeText(this, "Compartilhar clicado!", Toast.LENGTH_SHORT).show()
            R.id.nav_categories -> Toast.makeText(this, "Categorias clicado!", Toast.LENGTH_SHORT).show()
            R.id.nav_default_items -> Toast.makeText(this, "Itens padrão clicado!", Toast.LENGTH_SHORT).show()
            R.id.nav_support -> Toast.makeText(this, "Suporte clicado!", Toast.LENGTH_SHORT).show()
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showDeleteConfirmationDialog(list: ShoppingList) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Excluir Lista?")
            .setMessage("Tem certeza que deseja excluir a lista '${list.name}' e todos os seus itens? Esta ação não pode ser desfeita.")
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Excluir") { dialog, _ ->
                shoppingListViewModel.delete(list)
                Toast.makeText(this, "Lista '${list.name}' excluída.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .show()
    }

    private fun showEditListDialog(shoppingList: ShoppingList) {
        val input = EditText(this)
        input.setText(shoppingList.name)
        input.hint = "Novo nome da lista"

        AlertDialog.Builder(this)
            .setTitle("Editar Lista")
            .setView(input)
            .setPositiveButton("Salvar") { dialog, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val updatedList = shoppingList.copy(name = newName)
                    shoppingListViewModel.update(updatedList)
                    Toast.makeText(this, "Lista atualizada para '$newName'!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "O nome da lista não pode estar vazio.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onNewListCreated(listName: String) {
        val newList = ShoppingList(name = listName, date = LocalDate.now())
        shoppingListViewModel.insert(newList)
        Toast.makeText(this, "Nova lista criada: ${newList.name}", Toast.LENGTH_LONG).show()
    }

    private fun updateUIBasedOnLists(isEmpty: Boolean) {
        if (isEmpty) {
            Log.d(TAG, "Lista de compras vazia. Exibindo tela vazia.")
            binding.imageViewBasket.visibility = View.VISIBLE
            binding.textViewTitle.visibility = View.VISIBLE
            binding.textViewSubtitle.visibility = View.VISIBLE
            binding.recyclerViewLists.visibility = View.GONE
        } else {
            Log.d(TAG, "Listas de compras existentes. Exibindo RecyclerView.")
            binding.imageViewBasket.visibility = View.GONE
            binding.textViewTitle.visibility = View.GONE
            binding.textViewSubtitle.visibility = View.GONE
            binding.recyclerViewLists.visibility = View.VISIBLE
        }
    }
}