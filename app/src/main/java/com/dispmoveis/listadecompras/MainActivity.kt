package com.dispmoveis.listadecompras

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.dispmoveis.listadecompras.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener,
    AddListBottomSheetFragment.AddListListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle

    // --- NOVO: Lista para armazenar as listas de compras ---
    private val shoppingLists = mutableListOf<ShoppingList>()

    // --- NOVO: Adaptador do RecyclerView ---
    private lateinit var shoppingListAdapter: ShoppingListAdapter

    companion object { // <<--- Adicione este bloco
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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

        // --- NOVO: Configuração do RecyclerView ---
        // 1. Inicializa o Adapter, passando a lista de dados e os listeners de clique
        shoppingListAdapter = ShoppingListAdapter(
            shoppingLists, // A lista vazia inicialmente
            onItemClick = { clickedList ->
                // Lógica quando um item da lista é clicado (abrir detalhes da lista, por exemplo)
                val intent = android.content.Intent(this, ShoppingListDetailActivity::class.java)
                intent.putExtra("LIST_NAME", clickedList.name) // Passa o nome da lista para a próxima Activity
                startActivity(intent)
            },
            onEditClick = { editedList ->
                // Lógica quando o botão de editar de uma lista é clicado
                Toast.makeText(this, "Editar lista '${editedList.name}'", Toast.LENGTH_SHORT).show()
                // FUTURO: Abrir um Bottom Sheet ou Activity para editar o nome da lista
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

    // --- Implementação do listener do AddListBottomSheetFragment (modificado) ---
    override fun onNewListCreated(listName: String) {
        val newList = ShoppingList(name = listName) // Cria um novo objeto ShoppingList
        shoppingLists.add(newList) // Adiciona à sua lista de dados

        // Log para verificar se a lista foi adicionada
        Log.d(TAG, "Lista adicionada: ${newList.name}. Total de listas: ${shoppingLists.size}")

        shoppingListAdapter.updateLists(shoppingLists) // Notifica o Adapter sobre a mudança
        // Log para verificar se o Adapter foi notificado

        Log.d(TAG, "shoppingListAdapter.updateLists() chamado.")

        updateUIBasedOnLists() // Atualiza a visibilidade da tela

        // Log para verificar se a UI foi atualizada
        Log.d(TAG, "updateUIBasedOnLists() chamado.")

        Toast.makeText(this, "Nova lista criada: ${newList.name}", Toast.LENGTH_LONG).show()

        //ver o conteúdo completo da lista no log
        Log.d(TAG, "Conteúdo atual da shoppingLists: $shoppingLists")
    }

    // --- NOVO MÉTODO: Controla a visibilidade da tela vazia vs. RecyclerView ---
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