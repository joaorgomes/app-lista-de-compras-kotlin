package com.dispmoveis.listadecompras

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dispmoveis.listadecompras.databinding.ItemSuggestedProductBinding

class SuggestedProductAdapter(
    private var suggestedProducts: List<String>, // Lista de nomes de produtos sugeridos
    private val onAddClick: (String) -> Unit // Callback para quando o botão '+' é clicado
) : RecyclerView.Adapter<SuggestedProductAdapter.SuggestedProductViewHolder>() {

    inner class SuggestedProductViewHolder(private val binding: ItemSuggestedProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(productName: String) {
            binding.textViewSuggestedItemName.text = productName
            binding.imageViewAddSuggestedItem.setOnClickListener {
                onAddClick(productName)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestedProductViewHolder {
        val binding = ItemSuggestedProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SuggestedProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SuggestedProductViewHolder, position: Int) {
        holder.bind(suggestedProducts[position])
    }

    override fun getItemCount(): Int = suggestedProducts.size

    // Método para atualizar a lista de sugestões (ex: ao pesquisar)
    fun updateSuggestions(newSuggestions: List<String>) {
        suggestedProducts = newSuggestions
        notifyDataSetChanged()
    }
}