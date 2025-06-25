package com.dispmoveis.listadecompras

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.dispmoveis.listadecompras.databinding.BottomSheetAddListBinding // Importe a classe de binding gerada
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddListBottomSheetFragment : BottomSheetDialogFragment() {

    // Variável para o View Binding
    private var _binding: BottomSheetAddListBinding? = null
    // Propriedade para acessar o binding de forma segura
    private val binding get() = _binding!!

    // Listener para comunicação com a Activity/Fragment que abriu este Bottom Sheet
    interface AddListListener {
        fun onNewListCreated(listName: String)
    }

    private var addListListener: AddListListener? = null

    // Método para configurar o listener
    fun setAddListListener(listener: AddListListener) {
        this.addListListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Infla o layout do Bottom Sheet usando View Binding
        _binding = BottomSheetAddListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configura o listener para o botão de fechar (X)
        binding.imgCloseBottomSheet.setOnClickListener {
            dismiss() // Fecha o Bottom Sheet
        }

        // Configura o listener para o botão "Criar lista"
        binding.btnCreateList.setOnClickListener {
            val listName = binding.edtListName.text.toString().trim() // Obtém o texto e remove espaços em branco

            if (listName.isNotEmpty()) {
                // Notifica a Activity/Fragment pai sobre a nova lista
                addListListener?.onNewListCreated(listName)
                dismiss() // Fecha o Bottom Sheet após criar a lista
            } else {
                // Exibe uma mensagem se o nome da lista estiver vazio
                Toast.makeText(requireContext(), "Por favor, insira um nome para a lista.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Garante que o binding seja anulado quando a View do Fragment for destruída,
    // para evitar vazamentos de memória.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Opcional: Define um estilo para o Bottom Sheet para ter cantos arredondados
    override fun getTheme(): Int = R.style.BottomSheetDialogTheme // <<-- Você vai criar este estilo
}