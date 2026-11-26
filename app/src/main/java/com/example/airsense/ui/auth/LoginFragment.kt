package com.example.airsense.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.airsense.R
import com.example.airsense.data.auth.AuthRepository
import com.example.airsense.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val authRepository = AuthRepository()

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

        if (authRepository.currentUser != null) {
            findNavController().navigate(R.id.action_loginFragment_to_navigation_home)
            return
        }

        binding.btnGoogleSignIn.setOnClickListener {
            startGoogleSignIn()
        }
    }

    private fun startGoogleSignIn() {
        val webClientId = getString(R.string.default_web_client_id)
        if (webClientId.isBlank()) {
            Toast.makeText(
                requireContext(),
                "Falta configurar app/google-services.json para iniciar sesión con Google.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        binding.progressLogin.visibility = View.VISIBLE
        binding.btnGoogleSignIn.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val result = authRepository.signInWithGoogle(
                requireActivity(),
                webClientId
            )

            _binding?.let { currentBinding ->
                currentBinding.progressLogin.visibility = View.GONE
                currentBinding.btnGoogleSignIn.isEnabled = true
            }

            if (_binding == null || !isAdded) return@launch

            result.onSuccess {
                findNavController().navigate(R.id.action_loginFragment_to_navigation_home)
            }.onFailure { exception ->
                when (exception) {
                    is GetCredentialCancellationException -> {
                        // Cancelación voluntaria por parte del usuario: se maneja silenciosamente
                    }
                    is NoCredentialException -> {
                        Toast.makeText(
                            requireContext(),
                            "No se encontró ninguna cuenta de Google configurada en este dispositivo.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else -> {
                        Toast.makeText(
                            requireContext(),
                            "Error al iniciar sesión con Google. Por favor, inténtalo de nuevo.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
