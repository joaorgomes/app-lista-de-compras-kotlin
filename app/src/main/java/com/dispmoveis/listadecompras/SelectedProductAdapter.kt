package com.dispmoveis.listadecompras

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dispmoveis.listadecompras.databinding.ItemSelectedProductBinding

class SelectedProductAdapter(
    private val selectedProducts: MutableList<ProductItem>, // Lista de ProductItem
    private val onQuantityChange: (ProductItem, Int) -> Unit, // Callback: item, nova quantidade
    private val onRemoveClick: (ProductItem) -> Unit // Callback: item a ser removido
) : RecyclerView.Adapter<SelectedProductAdapter.SelectedProductViewHolder>() {

    inner class SelectedProductViewHolder(private val binding: ItemSelectedProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductItem) {
            binding.textViewSelectedProductName.text = product.name
            binding.textViewQuantity.text = product.quantity.toString()

            // Atualizar o texto da quantidade customizada se existir
            product.customQuantityText?.let {
                if (it.isNotEmpty()) {
                    binding.textViewQuantity.text = it // Exibe o texto customizado
                }
            }

            // Botão de diminuir quantidade
            binding.imageViewDecreaseQuantity.setOnClickListener {
                if (product.quantity > 1) { // Garante que a quantidade não seja menor que 1
                    product.quantity--
                    // Se houver texto de quantidade customizada, limpe-o ao diminuir
                    // para forçar a exibição do número
                    product.customQuantityText = null
                    onQuantityChange(product, product.quantity)
                    notifyItemChanged(adapterPosition) // Notifica que este item mudou
                } else if (product.quantity == 1) {
                    // Se for 1, remove o item
                    onRemoveClick(product)
                }
            }

            // Botão de aumentar quantidade
            binding.imageViewIncreaseQuantity.setOnClickListener {
                product.quantity++
                // Se houver texto de quantidade customizada, limpe-o ao aumentar
                product.customQuantityText = null
                onQuantityChange(product, product.quantity)
                notifyItemChanged(adapterPosition) // Notifica que este item mudou
            }

            // Botão de remover completamente (o 'X' vermelho na imagem)
            binding.imageViewRemoveSelectedItem.setOnClickListener {
                onRemoveClick(product)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectedProductViewHolder {
        val binding = ItemSelectedProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SelectedProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SelectedProductViewHolder, position: Int) {
        holder.bind(selectedProducts[position])
    }

    override fun getItemCount(): Int = selectedProducts.size

    // Métodos de manipulação da lista:

    // Adiciona um novo item ou incrementa quantidade se já existir
    fun addOrUpdateItem(newItem: ProductItem) {
        val existingItem = selectedProducts.find { it.name == newItem.name }
        if (existingItem != null) {
            existingItem.quantity++ // Incrementa a quantidade
            existingItem.customQuantityText = null // Limpa texto customizado
            notifyItemChanged(selectedProducts.indexOf(existingItem))
        } else {
            selectedProducts.add(newItem)
            notifyItemInserted(selectedProducts.size - 1)
        }
        // Chama o callback para que a Activity possa atualizar a UI (visibilidade do RecyclerView)
        onQuantityChange(newItem, newItem.quantity)
    }

    // Remove um item da lista
    fun removeItem(item: ProductItem) {
        val position = selectedProducts.indexOf(item)
        if (position != -1) {
            selectedProducts.removeAt(position)
            notifyItemRemoved(position)
        }

    }

    // Retorna a lista atual de itens selecionados
    fun getSelectedItems(): List<ProductItem> {
        return selectedProducts
    }
}