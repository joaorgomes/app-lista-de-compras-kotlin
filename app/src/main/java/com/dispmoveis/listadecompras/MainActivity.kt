package com.dispmoveis.listadecompras

import android.app.Activity // Importar Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts // Importar ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.dispmoveis.listadecompras.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder // NOVO: Importar para o AlertDialog

class MainActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener,
    AddListBottomSheetFragment.AddListListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle

    // --- Lista para armazenar as listas de compras ---
    private val shoppingLists = mutableListOf<ShoppingList>()

    // --- Adaptador do RecyclerView ---
    private lateinit var shoppingListAdapter: ShoppingListAdapter

    companion object {
        private const val TAG = "MainActivity"
    }

    // ActivityResultLauncher para editar listas
    private val editListLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            // Usar a constante definida em EditShoppingListActivity para a chave
            val editedList = data?.getParcelableExtra<ShoppingList>(EditShoppingListActivity.EXTRA_EDITED_SHOPPING_LIST)

            editedList?.let { updatedList ->
                Log.d(TAG, "Lista editada recebida: ${updatedList.name}, ID: ${updatedList.id}")

                // Encontre a lista original na sua 'shoppingLists' pelo ID e atualize-a
                val index = shoppingLists.indexOfFirst { it.id == updatedList.id }
                if (index != -1) {
                    shoppingLists[index] = updatedList // Substitui o objeto antigo pelo novo
                    shoppingListAdapter.updateLists(shoppingLists) // Notifica o adapter
                    Toast.makeText(this, "Lista '${updatedList.name}' atualizada!", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Lista '${updatedList.name}' atualizada em memória.")
                } else {
                    Log.e(TAG, "Lista editada não encontrada na lista principal: ${updatedList.name}, ID: ${updatedList.id}")
                    Toast.makeText(this, "Erro ao atualizar a lista.", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Log.d(TAG, "Nenhuma lista editada recebida.")
            }
        } else {
            Log.d(TAG, "Edição de lista cancelada ou falhou.")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /*ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }*/

        // --- Configuração da Toolbar (existente) ---
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.app_name)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)
        }

        // --- Configuração do DrawerLayout e NavigationView (existente) ---
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

        // --- Configuração do RecyclerView ---
        // 1. Inicializa o Adapter, passando a lista de dados e os listeners de clique
        shoppingListAdapter = ShoppingListAdapter(
            shoppingLists, // A lista vazia inicialmente
            onItemClick = { clickedList ->
                // Lógica quando um item da lista é clicado (abrir detalhes da lista, por exemplo)
                val intent = Intent(this, ShoppingListDetailActivity::class.java)
                intent.putExtra("LIST_NAME", clickedList.name) // Passa o nome da lista para a próxima Activity
                // É uma boa prática passar o ID também se você for usar ele na tela de detalhes
                intent.putExtra("LIST_ID", clickedList.id)
                startActivity(intent)
            },
            onEditClick = { listToEdit ->
                // Lógica quando o botão de editar de uma lista é clicado
                Log.d(TAG, "Clicou em editar lista: ${listToEdit.name}, ID: ${listToEdit.id}")
                val intent = Intent(this, EditShoppingListActivity::class.java).apply {
                    // Usar a constante definida em EditShoppingListActivity para a chave
                    putExtra(EditShoppingListActivity.EXTRA_ORIGINAL_SHOPPING_LIST, listToEdit)
                }
                editListLauncher.launch(intent) // Usa o launcher para iniciar a Activity de edição
            },
            onDeleteClick = { listToDelete -> // NOVO: Implementação do onDeleteClick
                Log.d(TAG, "Clicou em deletar lista: ${listToDelete.name}, ID: ${listToDelete.id}")
                showDeleteConfirmationDialog(listToDelete)
            }
        )

        // 2. Configura o RecyclerView
        binding.recyclerViewLists.apply {
            layoutManager = LinearLayoutManager(this@MainActivity) // Define um layout linear (lista vertical)
            adapter = shoppingListAdapter // Atribui o adaptador
        }

        // --- NOVO: Chama o método para atualizar a visibilidade da tela (vazia ou com listas) no início ---
        updateUIBasedOnLists()


        // --- Configuração do Floating Action Button (FAB) (existente) ---
        binding.fabAddList.setOnClickListener {
            val addListBottomSheet = AddListBottomSheetFragment()
            addListBottomSheet.setAddListListener(this)
            addListBottomSheet.show(supportFragmentManager, addListBottomSheet.tag)
        }
    }

    // --- onBackPressed (existente) ---
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    // --- onCreateOptionsMenu (existente) ---
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Não há itens de menu na toolbar principal por padrão, apenas o ícone do Drawer
        return true
    }

    // --- onOptionsItemSelected (existente) ---
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // --- onNavigationItemSelected (existente) ---
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_rate_us -> {
                Toast.makeText(this, "Avalie-nos clicado!", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_share -> {
                Toast.makeText(this, "Compartilhar clicado!", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_categories -> {
                Toast.makeText(this, "Categorias clicado!", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_default_items -> {
                Toast.makeText(this, "Itens padrão clicado!", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_support -> {
                Toast.makeText(this, "Suporte clicado!", Toast.LENGTH_SHORT).show()
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    // --- NOVO: Função para exibir o diálogo de confirmação de exclusão ---
    private fun showDeleteConfirmationDialog(list: ShoppingList) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Excluir Lista?")
            .setMessage("Tem certeza que deseja excluir a lista '${list.name}'? Esta ação não pode ser desfeita.")
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Excluir") { dialog, _ ->
                deleteShoppingList(list)
                dialog.dismiss()
            }
            .show()
    }

    // NOVO: Função para realmente remover a lista
    private fun deleteShoppingList(listToDelete: ShoppingList) {
        val removed = shoppingLists.remove(listToDelete) // Remove da lista mutável
        if (removed) {
            shoppingListAdapter.updateLists(shoppingLists) // Notifica o adapter
            updateUIBasedOnLists() // Atualiza a visibilidade (mostra placeholder se vazia)
            Toast.makeText(this, "Lista '${listToDelete.name}' excluída.", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Lista '${listToDelete.name}' (ID: ${listToDelete.id}) removida com sucesso. Total de listas: ${shoppingLists.size}")
            // FUTURO: Aqui você chamaria seu método para deletar do banco de dados
        } else {
            Log.e(TAG, "Erro: Lista '${listToDelete.name}' (ID: ${listToDelete.id}) não encontrada para exclusão.")
            Toast.makeText(this, "Erro ao excluir a lista.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Implementação do listener do AddListBottomSheetFragment ---
    override fun onNewListCreated(listName: String) {
        val newList = ShoppingList(name = listName) // Cria um novo objeto ShoppingList
        shoppingLists.add(newList) // Adiciona à sua lista de dados

        // Log para verificar se a lista foi adicionada
        Log.d(TAG, "Lista adicionada: ${newList.name}, ID: ${newList.id}. Total de listas: ${shoppingLists.size}")

        shoppingListAdapter.updateLists(shoppingLists) // Notifica o Adapter sobre a mudança

        Log.d(TAG, "shoppingListAdapter.updateLists() chamado.")

        updateUIBasedOnLists() // Atualiza a visibilidade da tela

        Log.d(TAG, "updateUIBasedOnLists() chamado.")

        Toast.makeText(this, "Nova lista criada: ${newList.name}", Toast.LENGTH_LONG).show()

        //ver o conteúdo completo da lista no log
        Log.d(TAG, "Conteúdo atual da shoppingLists: $shoppingLists")
    }

    // --- Método para controlar a visibilidade da tela vazia vs. RecyclerView ---
    private fun updateUIBasedOnLists() {
        if (shoppingLists.isEmpty()) {
            // Se não houver listas, mostra a mensagem de tela vazia e oculta o RecyclerView
            Log.d(TAG, "Lista de compras vazia. Exibindo tela vazia.")
            binding.imageViewBasket.visibility = View.VISIBLE
            binding.textViewTitle.visibility = View.VISIBLE
            binding.textViewSubtitle.visibility = View.VISIBLE
            binding.recyclerViewLists.visibility = View.GONE
        } else {
            // Se houver listas, oculta a mensagem de tela vazia e mostra o RecyclerView
            Log.d(TAG, "Listas de compras existentes (${shoppingLists.size}). Exibindo RecyclerView.")
            binding.imageViewBasket.visibility = View.GONE
            binding.textViewTitle.visibility = View.GONE
            binding.textViewSubtitle.visibility = View.GONE
            binding.recyclerViewLists.visibility = View.VISIBLE
        }
    }
}