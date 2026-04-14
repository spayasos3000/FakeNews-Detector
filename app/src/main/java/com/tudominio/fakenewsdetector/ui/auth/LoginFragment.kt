package com.tudominio.fakenewsdetector.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.tudominio.fakenewsdetector.R
import com.tudominio.fakenewsdetector.databinding.FragmentLoginBinding
import com.tudominio.fakenewsdetector.ui.auth.AuthState
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()
    
    private lateinit var oneTapLauncher: ActivityResultLauncher<BeginSignInRequest>
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupGoogleSignIn()
        setupListeners()
        observeAuthState()
    }

    private fun setupGoogleSignIn() {
        oneTapLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            try {
                val credential = Identity.getSignInClient(requireActivity())
                    .getSignInCredentialFromIntent(result.data)
                val idToken = credential.googleIdToken
                
                if (idToken != null) {
                    authViewModel.loginWithGoogle(idToken)
                } else {
                    showError("Error al obtener credenciales de Google")
                }
            } catch (e: ApiException) {
                showError("Error en el inicio de sesión con Google: ${e.statusCode}")
            }
        }

        signInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                
                if (idToken != null) {
                    authViewModel.loginWithGoogle(idToken)
                } else {
                    showError("Error al obtener token de Google")
                }
            } catch (e: ApiException) {
                showError("Error en el inicio de sesión con Google: ${e.statusCode}")
            }
        }
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (validateInputs(email, password)) {
                authViewModel.login(email, password)
            }
        }

        binding.btnRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.btnGoogleSignIn.setOnClickListener {
            launchGoogleSignIn()
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

    private fun launchGoogleSignIn() {
        try {
            // Opción 1: Usar One Tap (más moderno)
            val oneTapRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                    BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId(getString(R.string.google_web_client_id))
                        .setFilterByAuthorizedAccounts(false)
                        .build()
                )
                .setAutoSelectEnabled(false)
                .build()

            oneTapLauncher.launch(oneTapRequest)
        } catch (e: Exception) {
            // Fallback a Google Sign-In tradicional si One Tap falla
            try {
                val gso = authViewModel.getGoogleSignInOptions()
                val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
                val signInIntent = googleSignInClient.signInIntent
                signInLauncher.launch(signInIntent)
            } catch (e2: Exception) {
                showError("Error al configurar Google Sign-In: ${e2.message}")
            }
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            binding.tilEmail.error = "El email es requerido"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Email inválido"
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "La contraseña es requerida"
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "La contraseña debe tener al menos 6 caracteres"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        return isValid
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.isVisible = show
        binding.btnLogin.isEnabled = !show
        binding.btnRegister.isEnabled = !show
        binding.btnGoogleSignIn.isEnabled = !show
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun navigateToMain() {
        findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
