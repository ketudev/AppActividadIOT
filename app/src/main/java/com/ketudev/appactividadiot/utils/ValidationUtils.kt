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

    /**
     * Resultado del cruce de validaciones del formulario de luz.
     * [esValido] es true cuando ningún campo reporta error.
     */
    data class LuzFormErrors(
        val habitacion: String?,
        val watts: String?,
        val estado: String?
    ) {
        val esValido: Boolean get() = habitacion == null && watts == null && estado == null

        /** Primer error encontrado, para mostrar en un Snackbar si hiciese falta. */
        val primerError: String? get() = habitacion ?: watts ?: estado
    }

    /**
     * Determina si el formulario fue dejado completamente en blanco.
     * En ese caso la app permite crear/guardar un registro vacío (placeholder),
     * que luego puede completarse editándolo.
     */
    fun esFormularioVacio(habitacion: String, watts: String, estado: String): Boolean =
        habitacion.isBlank() && watts.isBlank() && estado.isBlank()

    /**
     * Aplica las validaciones lógicas de cada campo.
     * Regla: si el formulario está totalmente vacío NO se reportan errores
     * (registro vacío permitido). Si al menos un campo tiene contenido,
     * se validan los campos obligatorios con normalidad.
     */
    fun validateLuzForm(habitacion: String, watts: String, estado: String): LuzFormErrors {
        if (esFormularioVacio(habitacion, watts, estado)) {
            return LuzFormErrors(null, null, null)
        }
        return LuzFormErrors(
            habitacion = validateHabitacion(habitacion),
            watts = validateWatts(watts),
            estado = validateEstado(estado)
        )
    }
}
