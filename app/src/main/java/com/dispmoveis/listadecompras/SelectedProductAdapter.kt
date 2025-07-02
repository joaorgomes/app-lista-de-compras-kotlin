package com.dispmoveis.listadecompras

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dispmoveis.listadecompras.databinding.ItemSelectedProductBinding


class SelectedProductAdapter(
    // AGORA RECEBE E MANIPULA UMA LISTA DE ShoppingItem

    // Callbacks agora recebem ShoppingItem
    private val onQuantityChange: (ShoppingItem) -> Unit,
    private val onRemoveClick: (ShoppingItem) -> Unit
) : RecyclerView.Adapter<SelectedProductAdapter.SelectedProductViewHolder>() {

    private var selectedItems: MutableList<ShoppingItem> = mutableListOf()
    inner class SelectedProductViewHolder(private val binding: ItemSelectedProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ShoppingItem) { // AGORA RECEBE ShoppingItem
            binding.textViewSelectedProductName.text = item.name
            // Exibe a quantidade ou o texto customizado
            if (item.customQuantityText != null && item.customQuantityText!!.isNotEmpty()) {
                binding.textViewQuantity.text = item.customQuantityText
            } else {
                binding.textViewQuantity.text = item.quantity.toString()
            }


            // Botão de diminuir quantidade
            binding.imageViewDecreaseQuantity.setOnClickListener {
                if (item.quantity > 1) {
                    item.quantity--
                    item.customQuantityText = null // Limpa texto customizado se usar o contador numérico
                    Log.d("SelectedProductAdapter", "DEBUG_ADAPTER_QTD: Diminuindo qtd de '${item.name}' para ${item.quantity}. Posição: $adapterPosition")
                    onQuantityChange(item) // Notifica a Activity
                    notifyItemChanged(adapterPosition) // Notifica que este item no adapter mudou
                } else if (item.quantity == 1) {
                    Log.d("SelectedProductAdapter", "DEBUG_ADAPTER_QTD: Removendo '${item.name}' (qtd 1, decrease clicado). Posição: $adapterPosition")
                    onRemoveClick(item) // Notifica a Activity para remover
                }
            }

            // Botão de aumentar quantidade
            binding.imageViewIncreaseQuantity.setOnClickListener {
                item.quantity++
                item.customQuantityText = null // Limpa texto customizado
                Log.d("SelectedProductAdapter", "DEBUG_ADAPTER_QTD: Aumentando qtd de '${item.name}' para ${item.quantity}. Posição: $adapterPosition")
                onQuantityChange(item) // Notifica a Activity
                notifyItemChanged(adapterPosition) // Notifica que este item no adapter mudou
            }

            // Botão de remover completamente (o 'X' vermelho na imagem)
            binding.imageViewRemoveSelectedItem.setOnClickListener {
                Log.d("SelectedProductAdapter", "DEBUG_ADAPTER_REMOVE: Clicado remover para '${item.name}'. Posição: $adapterPosition")
                onRemoveClick(item) // Notifica a Activity para remover
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectedProductViewHolder {
        val binding = ItemSelectedProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SelectedProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SelectedProductViewHolder, position: Int) {
        // Certifique-se de que a posição é válida
        if (position < selectedItems.size) {
            holder.bind(selectedItems[position])
            Log.d("SelectedProductAdapter", "DEBUG_BIND: Bindando item na posição $position: '${selectedItems[position].name}', Qtd: ${selectedItems[position].quantity}")
        } else {
            Log.e("SelectedProductAdapter", "DEBUG_BIND: Tentativa de bindar posição $position, mas selectedItems.size é ${selectedItems.size}. Isso é um erro de índice!")
        }
    }

    override fun getItemCount(): Int {
        val count = selectedItems.size
        Log.d("SelectedProductAdapter", "DEBUG_ADAPTER_COUNT: getItemCount chamado. Retornando: $count")
        return count
    }

    // Método crucial para atualizar a lista do adaptador e notificar a UI
    fun updateList(newItems: List<ShoppingItem>) {
        Log.d("SelectedProductAdapter", "DEBUG_ADAPTER_UPDATE: updateList chamado. newItems size recebido: ${newItems.size}")
        // Limpa a lista existente e adiciona todos os novos itens
        selectedItems.clear()
        selectedItems.addAll(newItems)
        notifyDataSetChanged() // Notifica o RecyclerView que o dataset completo mudou
        Log.d("SelectedProductAdapter", "DEBUG_ADAPTER_UPDATE: Lista interna do adaptador AGORA tem: ${selectedItems.size} itens.")
        selectedItems.forEachIndexed { index, item ->
            Log.d("SelectedProductAdapter", "DEBUG_ADAPTER_UPDATE: Item interno[$index]: Name='${item.name}', Qtd=${item.quantity}")
        }
    }
}