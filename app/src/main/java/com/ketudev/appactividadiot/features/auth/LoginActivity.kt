package com.ketudev.appactividadiot.features.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.snackbar.Snackbar
import com.ketudev.appactividadiot.MainActivity
import com.ketudev.appactividadiot.R
import com.ketudev.appactividadiot.databinding.ActivityLoginBinding
import com.ketudev.appactividadiot.models.AuthResult
import com.ketudev.appactividadiot.utils.AuthSource
import com.ketudev.appactividadiot.utils.ErrorSanitizer
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if user is already logged in
        if (viewModel.isUserLoggedIn()) {
            navigateToMain()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            viewModel.signInWithEmail(email, password)
        }

        binding.btnGoogleSignIn.setOnClickListener {
            launchGoogleSignIn()
        }

        binding.btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.authResult.observe(this) { result ->
            when (result) {
                is AuthResult.Idle -> setLoading(false)
                is AuthResult.Loading -> setLoading(true)
                is AuthResult.Success -> {
                    setLoading(false)
                    navigateToMain()
                }
                is AuthResult.Error -> {
                    setLoading(false)
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                    viewModel.resetState()
                }
            }
        }

        viewModel.emailError.observe(this) { error ->
            binding.tilEmail.error = error
        }

        viewModel.passwordError.observe(this) { error ->
            binding.tilPassword.error = error
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.btnGoogleSignIn.isEnabled = !loading
        binding.etEmail.isEnabled = !loading
        binding.etPassword.isEnabled = !loading
    }

    private fun launchGoogleSignIn() {
        requestGoogleCredential(onlyAuthorizedAccounts = false)
    }

    /**
     * Solicita el credential de Google. Primero se ofrece el selector de cuentas ya
     * autorizadas en el dispositivo (selector nativo) y, si no hay ninguna, se reintenta
     * con el selector completo para cuentas nuevas.
     */
    private fun requestGoogleCredential(onlyAuthorizedAccounts: Boolean) {
        val credentialManager = CredentialManager.create(this)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(onlyAuthorizedAccounts)
            .setServerClientId(getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val response = credentialManager.getCredential(
                    request = request,
                    context = this@LoginActivity
                )
                handleGoogleSignIn(response)
            } catch (e: GetCredentialCancellationException) {
                showSnackbar("Operación cancelada por el usuario.")
            } catch (e: NoCredentialException) {
                if (onlyAuthorizedAccounts) {
                    // No hay cuentas autorizadas: se ofrece el selector completo.
                    requestGoogleCredential(onlyAuthorizedAccounts = false)
                } else {
                    showSnackbar(
                        "No se encontró una cuenta de Google en este dispositivo."
                    )
                }
            } catch (e: GetCredentialProviderConfigurationException) {
                Log.e(TAG, "Proveedor de credenciales mal configurado", e)
                showSnackbar(ErrorSanitizer.sanitize(e, AuthSource.GOOGLE))
            } catch (e: Exception) {
                Log.e(TAG, "Fallo en Google Sign-In", e)
                showSnackbar(ErrorSanitizer.sanitize(e, AuthSource.GOOGLE))
            }
        }
    }

    private fun handleGoogleSignIn(response: GetCredentialResponse) {
        when (val credential = response.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    viewModel.signInWithGoogle(googleIdTokenCredential.idToken)
                } else {
                    showSnackbar(getString(R.string.error_google_sign_in))
                }
            }

            else -> showSnackbar(getString(R.string.error_google_sign_in))
        }
    }

    private fun showSnackbar(message: String) {
        if (::binding.isInitialized) {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}
