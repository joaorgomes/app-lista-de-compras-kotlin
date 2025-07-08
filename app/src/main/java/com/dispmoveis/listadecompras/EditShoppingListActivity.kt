package com.dispmoveis.listadecompras

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dispmoveis.listadecompras.databinding.ActivityEditShoppingListBinding
import com.dispmoveis.listadecompras.model.ShoppingList

class EditShoppingListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditShoppingListBinding
    private var originalShoppingList: ShoppingList? = null // Para guardar a lista que veio para edição

    companion object {
        const val EXTRA_EDITED_SHOPPING_LIST = "EDITED_SHOPPING_LIST_RESULT"
        const val EXTRA_ORIGINAL_SHOPPING_LIST = "ORIGINAL_SHOPPING_LIST" // Chave para receber a lista
        private const val TAG = "EditShoppingListActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditShoppingListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbarEditList)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.edit_list_title)
        }
        binding.toolbarEditList.setNavigationOnClickListener {
            setResult(Activity.RESULT_CANCELED) // Se voltar sem salvar, envia RESULT_CANCELED
            finish()
        }

        // 1. Receber a lista de compras a ser editada
        originalShoppingList = intent.getParcelableExtra(EXTRA_ORIGINAL_SHOPPING_LIST)

        originalShoppingList?.let { list ->
            Log.d(TAG, "Recebendo lista para edição: ${list.name}, ID: ${list.id}")
            binding.editTextListName.setText(list.name) // Preenche o EditText com o nome atual
        } ?: run {
            Log.e(TAG, "Nenhuma ShoppingList recebida para edição.")
            Toast.makeText(this, "Erro: Nenhuma lista para editar.", Toast.LENGTH_SHORT).show()
            finish() // Fecha a activity se não houver lista
        }

        // 2. Configurar o botão Salvar
        binding.buttonSaveList.setOnClickListener {
            saveEditedList()
        }
    }

    private fun saveEditedList() {
        val newName = binding.editTextListName.text.toString().trim()

        if (newName.isEmpty()) {
            binding.textInputLayoutListName.error = "O nome da lista não pode estar vazio."
            return
        }

        originalShoppingList?.let { originalList ->
            val editedList = originalList.copy(name = newName) // Cria uma cópia com o novo nome
            Log.d(TAG, "Lista editada: Original ID: ${originalList.id}, Novo Nome: ${editedList.name}")

            val resultIntent = Intent().apply {
                putExtra(EXTRA_EDITED_SHOPPING_LIST, editedList)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    // Opcional: lida com o botão "voltar" do sistema (navegação por gesto/botão)
    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED) // Se o usuário voltar, cancela a operação
        super.onBackPressed()
    }
}