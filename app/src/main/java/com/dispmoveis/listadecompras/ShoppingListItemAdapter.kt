package com.dispmoveis.listadecompras

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dispmoveis.listadecompras.databinding.ItemListItemBinding

class ShoppingListItemAdapter(
    private var shoppingListItems: MutableList<ShoppingItem>, // Agora lista de ShoppingItem
    private val onItemClick: (ShoppingItem) -> Unit, // Callback para clique no item (passa ShoppingItem)
    private val onEditClick: (ShoppingItem) -> Unit, // Callback para clique em editar (passa ShoppingItem)
    private val onDeleteClick: (ShoppingItem) -> Unit // Callback para clique em deletar (passa ShoppingItem)
) : RecyclerView.Adapter<ShoppingListItemAdapter.ShoppingListItemViewHolder>() {

    inner class ShoppingListItemViewHolder(private val binding: ItemListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(shoppingItem: ShoppingItem) { // Agora recebe ShoppingItem
            binding.textViewItemName.text = shoppingItem.name

            // Lógica do Checkbox (marca/desmarca item como comprado)
            binding.checkboxItem.setOnCheckedChangeListener(null) // Limpa listener para evitar loops
            binding.checkboxItem.isChecked = shoppingItem.isPurchased // Define o estado do checkbox

            // Aplica/remove o risco no texto baseado no estado de compra
            if (shoppingItem.isPurchased) {
                binding.textViewItemName.paintFlags =
                    binding.textViewItemName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.textViewItemName.paintFlags =
                    binding.textViewItemName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            // Define o listener para o checkbox
            binding.checkboxItem.setOnCheckedChangeListener { _, isChecked ->
                shoppingItem.isPurchased = isChecked // Atualiza o estado no modelo de dados
                if (isChecked) {
                    binding.textViewItemName.paintFlags =
                        binding.textViewItemName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    binding.textViewItemName.paintFlags =
                        binding.textViewItemName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
                // Opcional: Se precisar notificar a Activity sobre a mudança de estado (para persistência, etc.)
                // onItemClick(shoppingItem) // Ou um callback específico para 'onCheckedChange'
            }

            // Clicks nos ícones e no item inteiro
            // Passa o objeto ShoppingItem completo nos callbacks
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
        val item = shoppingListItems[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = shoppingListItems.size

    // Método para atualizar a lista de itens (pode ser usado se a lista inteira mudar)
    fun updateItems(newItems: List<ShoppingItem>) {
        shoppingListItems.clear()
        shoppingListItems.addAll(newItems)
        notifyDataSetChanged() // Notifica uma mudança completa no dataset
    }

    // Adiciona um item e notifica o adapter
    fun addItem(item: ShoppingItem) { // Agora recebe ShoppingItem
        shoppingListItems.add(item)
        notifyItemInserted(shoppingListItems.size - 1)
        // notifyDataSetChanged() // Alternativa mais "pesada", mas garante a atualização
    }

    // Remove um item e notifica o adapter
    fun removeItem(item: ShoppingItem) { // Agora recebe ShoppingItem
        val position = shoppingListItems.indexOf(item)
        if (position != -1) {
            shoppingListItems.removeAt(position)
            notifyItemRemoved(position)
            // notifyDataSetChanged() // Alternativa mais "pesada"
        }
    }
}