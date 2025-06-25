package com.dispmoveis.listadecompras

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dispmoveis.listadecompras.databinding.ItemShoppingListBinding // Importe a classe de binding gerada

class ShoppingListAdapter(
    private var shoppingLists: List<ShoppingList>, // Sua lista de dados
    private val onItemClick: (ShoppingList) -> Unit, // Listener para clique no item
    private val onEditClick: (ShoppingList) -> Unit // Listener para clique no botão de editar
) : RecyclerView.Adapter<ShoppingListAdapter.ShoppingListViewHolder>() {

    // ViewHolder: responsável por manter as referências das Views de um item da lista
    inner class ShoppingListViewHolder(private val binding: ItemShoppingListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(shoppingList: ShoppingList) {
            binding.textViewListName.text = shoppingList.name
            // Por enquanto, o contador de itens é fixo, mas será dinâmico depois
            binding.textViewListItemCount.text = itemView.context.getString(R.string.item_count_placeholder, 0, 0)

            // Configura o clique no item completo
            binding.root.setOnClickListener {
                onItemClick(shoppingList)
            }

            // Configura o clique no ícone de editar
            binding.imageViewEditList.setOnClickListener {
                onEditClick(shoppingList)
            }
        }
    }

    // Cria e infla o layout de cada item (chamado quando o RecyclerView precisa de um novo ViewHolder)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingListViewHolder {
        val binding = ItemShoppingListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShoppingListViewHolder(binding)
    }

    // Associa os dados a um ViewHolder existente (chamado para preencher um ViewHolder com dados)
    override fun onBindViewHolder(holder: ShoppingListViewHolder, position: Int) {
        val shoppingList = shoppingLists[position]
        holder.bind(shoppingList)
    }

    // Retorna o número total de itens na lista de dados
    override fun getItemCount(): Int = shoppingLists.size

    // Método para atualizar os dados do Adapter e notificar o RecyclerView
    fun updateLists(newList: List<ShoppingList>) {
        this.shoppingLists = newList
        notifyDataSetChanged() // Notifica o RecyclerView que os dados mudaram e ele precisa se redesenhar
    }
}