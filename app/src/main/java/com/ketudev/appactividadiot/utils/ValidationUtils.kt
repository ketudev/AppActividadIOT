package com.ketudev.appactividadiot.utils

import android.util.Patterns

object ValidationUtils {

    fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "Ingresa un correo electrónico"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Ingresa un correo electrónico válido"
            else -> null
        }
    }

    fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Ingresa una contraseña"
            password.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
            else -> null
        }
    }

    fun validateConfirmPassword(password: String, confirmPassword: String): String? {
        return when {
            confirmPassword.isBlank() -> "Confirma tu contraseña"
            confirmPassword != password -> "Las contraseñas no coinciden"
            else -> null
        }
    }

    fun validateName(name: String): String? {
        return when {
            name.isBlank() -> "Ingresa tu nombre"
            else -> null
        }
    }
}
