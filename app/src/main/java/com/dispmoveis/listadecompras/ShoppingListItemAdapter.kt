package com.dispmoveis.listadecompras

import android.graphics.Paint
import android.icu.text.NumberFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dispmoveis.listadecompras.databinding.ItemListItemBinding
import java.util.Locale

class ShoppingListItemAdapter(

    private val onItemClick: (ShoppingItem) -> Unit,
    private val onEditClick: (ShoppingItem) -> Unit,
    private val onDeleteClick: (ShoppingItem) -> Unit
) : RecyclerView.Adapter<ShoppingListItemAdapter.ShoppingListItemViewHolder>() {

    private val itemsToDisplay: MutableList<ShoppingItem> = mutableListOf()

    inner class ShoppingListItemViewHolder(private val binding: ItemListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(shoppingItem: ShoppingItem) {
            binding.textViewItemName.text = shoppingItem.name

            // Exibe a quantidade
            if (shoppingItem.customQuantityText != null && shoppingItem.customQuantityText!!.isNotEmpty()) {
                binding.itemQuantityTextView.text = shoppingItem.customQuantityText
            } else {
                binding.itemQuantityTextView.text = "${shoppingItem.quantity}x"
            }

            // Exibe o preço
            val formattedPrice = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(shoppingItem.price)
            binding.itemPriceTextView.text = formattedPrice

            // Lógica do Checkbox (marca/desmarca item como comprado)
            binding.checkboxItem.setOnCheckedChangeListener(null)
            binding.checkboxItem.isChecked = shoppingItem.isPurchased

            Log.d("ShoppingListItemAdapter", "DEBUG_BIND_PURCHASED: Bindando '${shoppingItem.name}', isPurchased: ${shoppingItem.isPurchased}")
            // Aplica/remove o risco no texto baseado no estado de compra
            if (shoppingItem.isPurchased) {
                binding.textViewItemName.paintFlags =
                    binding.textViewItemName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.itemQuantityTextView.paintFlags =
                    binding.itemQuantityTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.itemPriceTextView.paintFlags =
                    binding.itemPriceTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.textViewItemName.paintFlags =
                    binding.textViewItemName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.itemQuantityTextView.paintFlags =
                    binding.itemQuantityTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.itemPriceTextView.paintFlags =
                    binding.itemPriceTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            binding.checkboxItem.setOnCheckedChangeListener { _, isChecked ->
                shoppingItem.isPurchased = isChecked
                // Atualiza o risco no texto imediatamente
                if (isChecked) {
                    binding.textViewItemName.paintFlags = binding.textViewItemName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    binding.itemQuantityTextView.paintFlags = binding.itemQuantityTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    binding.itemPriceTextView.paintFlags = binding.itemPriceTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    binding.textViewItemName.paintFlags = binding.textViewItemName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    binding.itemQuantityTextView.paintFlags = binding.itemQuantityTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    binding.itemPriceTextView.paintFlags = binding.itemPriceTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
                onItemClick(shoppingItem) // Chama o callback
            }

            // Clicks nos ícones e no item inteiro
            binding.root.setOnClickListener { onItemClick(shoppingItem) }
            binding.imageViewEditItem.setOnClickListener { onEditClick(shoppingItem) }
            binding.imageViewDeleteItem.setOnClickListener { onDeleteClick(shoppingItem) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingListItemViewHolder {
        val binding = ItemListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShoppingListItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShoppingListItemViewHolder, position: Int) {
        val item = itemsToDisplay[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = itemsToDisplay.size

    fun updateItems(newItems: List<ShoppingItem>) {
        itemsToDisplay.clear()
        itemsToDisplay.addAll(newItems)
        notifyDataSetChanged()
        Log.d("ShoppingListItemAdapter", "DEBUG_UPDATE: Lista do adapter atualizada com ${newItems.size} itens. Contagem atual: ${itemsToDisplay.size}")
        /*itemsToDisplay.forEachIndexed { index, item ->
            Log.d("ShoppingListItemAdapter", "DEBUG_UPDATE: Adapter Item[$index]: Name='${item.name}', Qtd=${item.quantity}")
        }*/
    }

    fun addItem(item: ShoppingItem) {
        // Verifica se o item já existe para não duplicar, apenas atualizar quantidade
        val existingItem = itemsToDisplay.find { it.name == item.name }
        if (existingItem != null) {
            existingItem.quantity += item.quantity
            existingItem.customQuantityText = item.customQuantityText // Atualiza texto customizado se houver
            notifyItemChanged(itemsToDisplay.indexOf(existingItem))
        } else {
            itemsToDisplay.add(item)
            notifyItemInserted(itemsToDisplay.size - 1)
        }
        Log.d("ShoppingListItemAdapter", "DEBUG_ADD: Item '${item.name}' adicionado/atualizado. Total: ${itemsToDisplay.size}")
    }

    fun removeItem(item: ShoppingItem) {
        val position = itemsToDisplay.indexOf(item)
        if (position != -1) {
            itemsToDisplay.removeAt(position)
            Log.d("ShoppingListItemAdapter", "DEBUG_REMOVE: Item '${item.name}' removido da posição $position. Total: ${itemsToDisplay.size}")
            notifyItemRemoved(position)
        }else {
            Log.w("ShoppingListItemAdapter", "DEBUG_REMOVE: Item '${item.name}' não encontrado para remoção.")
        }
    }
}