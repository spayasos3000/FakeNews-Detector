package com.tudominio.fakenewsdetector.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.tudominio.fakenewsdetector.databinding.FragmentRegisterBinding
import com.tudominio.fakenewsdetector.ui.auth.AuthState
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupListeners()
        observeAuthState()
    }

    private fun setupListeners() {
        binding.btnCreateAccount.setOnClickListener {
            val displayName = binding.etDisplayName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (validateInputs(displayName, email, password, confirmPassword)) {
                authViewModel.register(email, password, displayName)
            }
        }

        binding.tvLoginLink.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Loading -> {
                        showLoading(true)
                    }
                    is AuthState.Success -> {
                        showLoading(false)
                        navigateToMain()
                    }
                    is AuthState.Error -> {
                        showLoading(false)
                        showError(state.message)
                    }
                    is AuthState.Idle -> {
                        showLoading(false)
                    }
                }
            }
        }
    }

    private fun validateInputs(
        displayName: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        var isValid = true

        // Validar nombre
        if (displayName.isEmpty()) {
            binding.tilDisplayName.error = "El nombre es requerido"
            isValid = false
        } else if (displayName.length < 3) {
            binding.tilDisplayName.error = "El nombre debe tener al menos 3 caracteres"
            isValid = false
        } else {
            binding.tilDisplayName.error = null
        }

        // Validar email
        if (email.isEmpty()) {
            binding.tilEmail.error = "El email es requerido"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Email inválido"
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        // Validar contraseña
        if (password.isEmpty()) {
            binding.tilPassword.error = "La contraseña es requerida"
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "La contraseña debe tener al menos 6 caracteres"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        // Validar confirmación de contraseña
        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Confirma tu contraseña"
            isValid = false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Las contraseñas no coinciden"
            isValid = false
        } else {
            binding.tilConfirmPassword.error = null
        }

        return isValid
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.isVisible = show
        binding.btnCreateAccount.isEnabled = !show
        binding.tvLoginLink.isEnabled = !show
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun navigateToMain() {
        findNavController().navigate(R.id.action_registerFragment_to_mainFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
