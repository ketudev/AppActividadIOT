package com.ketudev.appactividadiot.utils

import android.util.Patterns

object ValidationUtils {

    fun validateEmail(email: String): String? {
        val trimmed = email.trim()
        return when {
            trimmed.isBlank() -> "Ingresa un correo electrónico"
            !Patterns.EMAIL_ADDRESS.matcher(trimmed).matches() -> "Ingresa un correo electrónico válido"
            trimmed.length > 100 -> "El correo no puede exceder los 100 caracteres"
            else -> null
        }
    }

    fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Ingresa una contraseña"
            password.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
            password.length > 128 -> "La contraseña no puede exceder los 128 caracteres"
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
        val trimmed = name.trim()
        return when {
            trimmed.isBlank() -> "Ingresa tu nombre completo"
            trimmed.length < 2 -> "El nombre debe tener al menos 2 caracteres"
            trimmed.length > 50 -> "El nombre no puede exceder los 50 caracteres"
            else -> null
        }
    }

    fun validateHabitacion(habitacion: String): String? {
        val trimmed = habitacion.trim()
        return when {
            trimmed.isBlank() -> "Ingresa el nombre de la habitación"
            trimmed.length < 2 -> "El nombre debe tener al menos 2 caracteres"
            trimmed.length > 100 -> "El nombre de la habitación no puede exceder los 100 caracteres"
            else -> null
        }
    }

    fun validateWatts(wattsStr: String): String? {
        val trimmed = wattsStr.trim()
        if (trimmed.isBlank()) {
            return "Ingresa el consumo en Watts"
        }
        val watts = trimmed.toIntOrNull()
            ?: return "Ingresa un número entero válido para Watts"
        return when {
            watts <= 0 -> "El consumo debe ser mayor a 0 W"
            watts > 150 -> "El consumo de la ampolleta no puede superar los 150 W"
            else -> null
        }
    }

    fun validateEstado(estado: String): String? {
        val trimmed = estado.trim()
        return when {
            trimmed.isBlank() -> "Selecciona un estado"
            !trimmed.equals("Encendida", ignoreCase = true) && !trimmed.equals("Apagada", ignoreCase = true) ->
                "Selecciona un estado válido"
            else -> null
        }
    }
}
