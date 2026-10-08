package com.ketudev.appactividadiot.features.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ketudev.appactividadiot.data.services.AuthService
import com.ketudev.appactividadiot.models.AuthResult
import com.ketudev.appactividadiot.utils.ErrorSanitizer
import com.ketudev.appactividadiot.utils.ValidationUtils
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val authService = AuthService()

    private val _authResult = MutableLiveData<AuthResult>(AuthResult.Idle)
    val authResult: LiveData<AuthResult> = _authResult

    private val _emailError = MutableLiveData<String?>()
    val emailError: LiveData<String?> = _emailError

    private val _passwordError = MutableLiveData<String?>()
    val passwordError: LiveData<String?> = _passwordError

    private val _confirmPasswordError = MutableLiveData<String?>()
    val confirmPasswordError: LiveData<String?> = _confirmPasswordError

    private val _nameError = MutableLiveData<String?>()
    val nameError: LiveData<String?> = _nameError

    fun isUserLoggedIn(): Boolean = authService.currentUser != null

    fun signInWithEmail(email: String, password: String) {
        // Validate
        val emailErr = ValidationUtils.validateEmail(email)
        val passwordErr = ValidationUtils.validatePassword(password)

        _emailError.value = emailErr
        _passwordError.value = passwordErr

        if (emailErr != null || passwordErr != null) return

        _authResult.value = AuthResult.Loading
        viewModelScope.launch {
            try {
                val user = authService.signInWithEmail(email, password)
                _authResult.value = AuthResult.Success(user)
            } catch (e: Exception) {
                _authResult.value = AuthResult.Error(
                    ErrorSanitizer.sanitize(e)
                )
            }
        }
    }

    fun createAccount(name: String, email: String, password: String, confirmPassword: String) {
        // Validate all fields
        val nameErr = ValidationUtils.validateName(name)
        val emailErr = ValidationUtils.validateEmail(email)
        val passwordErr = ValidationUtils.validatePassword(password)
        val confirmErr = ValidationUtils.validateConfirmPassword(password, confirmPassword)

        _nameError.value = nameErr
        _emailError.value = emailErr
        _passwordError.value = passwordErr
        _confirmPasswordError.value = confirmErr

        if (nameErr != null || emailErr != null || passwordErr != null || confirmErr != null) return

        _authResult.value = AuthResult.Loading
        viewModelScope.launch {
            try {
                val user = authService.createAccount(email, password, name)
                _authResult.value = AuthResult.Success(user)
            } catch (e: Exception) {
                _authResult.value = AuthResult.Error(
                    ErrorSanitizer.sanitize(e)
                )
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        _authResult.value = AuthResult.Loading
        viewModelScope.launch {
            try {
                val user = authService.signInWithGoogle(idToken)
                _authResult.value = AuthResult.Success(user)
            } catch (e: Exception) {
                _authResult.value = AuthResult.Error(
                    ErrorSanitizer.sanitize(e)
                )
            }
        }
    }

    fun signOut() {
        authService.signOut()
        _authResult.value = AuthResult.Idle
    }

    fun resetState() {
        _authResult.value = AuthResult.Idle
        _emailError.value = null
        _passwordError.value = null
        _confirmPasswordError.value = null
        _nameError.value = null
    }
}
