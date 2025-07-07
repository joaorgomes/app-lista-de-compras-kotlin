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
import com.dispmoveis.listadecompras.databinding.ActivityEditItemBinding

class EditItemActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditItemBinding
    private var originalItem: ShoppingItem? = null // Para guardar o item original

    companion object {
        // Define a constante aqui para que outras classes possam acessá-la
        const val EXTRA_EDITED_SHOPPING_ITEM_RESULT = "EDITED_SHOPPING_ITEM_RESULT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. Receber o ShoppingItem da Intent
        originalItem = intent.getParcelableExtra("SHOPPING_ITEM_TO_EDIT")

        originalItem?.let { item ->
            // Preencher os campos com os dados do item original
            binding.etItemName.setText(item.name)
            // Lidar com quantidade customizada ou numérica
            if (item.customQuantityText != null && item.customQuantityText!!.isNotEmpty()) {
                binding.etItemQuantity.setText(item.customQuantityText)
            } else {
                binding.etItemQuantity.setText(item.quantity.toString())
            }

            // Preencher o preço (se houver)
            if (item.price > 0.0) {
                binding.etItemPrice.setText(item.price.toString())
            }
            binding.cbIsPurchased.isChecked = item.isPurchased
        } ?: run {
            // Se não recebeu item, provavelmente é um erro (ou está criando novo, mas não é o caso aqui)
            Toast.makeText(this, "Erro: Item não encontrado para edição.", Toast.LENGTH_LONG).show()
            finish() // Fecha a activity se não tem item para editar
            return
        }

        binding.btnSave.setOnClickListener {
            saveEditedItem()
        }

        binding.btnCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED) // Indica que a edição foi cancelada
            finish()
        }
    }

    private fun saveEditedItem() {
        val name = binding.etItemName.text.toString().trim()
        val quantityText = binding.etItemQuantity.text.toString().trim()
        val priceText = binding.etItemPrice.text.toString().trim()

        if (name.isEmpty()) {
            binding.tilItemName.error = "O nome do item não pode estar vazio."
            return
        }
        binding.tilItemName.error = null // Limpa o erro se estiver ok

        var quantity: Int = 1
        var customQuantity: String? = null
        if (quantityText.matches(Regex("\\d+"))) { // Se é um número inteiro
            quantity = quantityText.toIntOrNull() ?: 1
        } else { // Se não é número, é texto customizado
            customQuantity = quantityText
            quantity = 1 // Por padrão, defina 1 para a quantidade numérica se for customizada
        }

        val price = priceText.toDoubleOrNull() ?: 0.0 // Converte para Double, 0.0 se for vazio/inválido

        val isPurchased = binding.cbIsPurchased.isChecked

        // Criar um novo ShoppingItem com os dados editados.
        // É importante manter o mesmo ID do original para poder atualizá-lo na lista principal.
        val editedItem = originalItem?.copy(
            name = name,
            quantity = quantity,
            customQuantityText = customQuantity,
            price = price,
            isPurchased = isPurchased
        )

        if (editedItem != null) {
            val resultIntent = Intent().apply {
                putExtra("EDITED_SHOPPING_ITEM_RESULT", editedItem)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        } else {
            Toast.makeText(this, "Erro ao salvar item. Item original ausente.", Toast.LENGTH_SHORT).show()
        }
    }
}