package com.dispmoveis.listadecompras

import java.util.UUID

/**
 * Data class que representa uma lista de compras individual.
 */
data class ShoppingList(
    val id: String = UUID.randomUUID().toString(), // ID único para a lista
    val name: String, // Nome da lista (ex: "Compras da Semana", "Churrasco")
    // val createdAt: Long = System.currentTimeMillis() // Exemplo: para adicionar data de criação
    // val itemCount: Int = 0 // Exemplo: para contar itens na lista
)