package com.ketudev.appactividadiot.utils

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException

object ErrorSanitizer {

    fun sanitize(throwable: Throwable?): String {
        if (throwable == null) return "Ocurrió un error inesperado."

        val message = throwable.message.orEmpty().lowercase()

        if (throwable is FirebaseNetworkException || message.contains("network error") || message.contains("unable to resolve host")) {
            return "Error de conexión. Verifica tu conexión a internet."
        }

        if (throwable is FirebaseAuthException) {
            return when (throwable.errorCode) {
                "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND", "invalid-credential", "wrong-password", "user-not-found" ->
                    "Correo o contraseña incorrectos."
                "ERROR_INVALID_EMAIL", "invalid-email" ->
                    "El formato del correo electrónico no es válido."
                "ERROR_EMAIL_ALREADY_IN_USE", "email-already-in-use" ->
                    "El correo electrónico ya se encuentra registrado."
                "ERROR_WEAK_PASSWORD", "weak-password" ->
                    "La contraseña es demasiado débil. Usa al menos 6 caracteres."
                "ERROR_USER_DISABLED", "user-disabled" ->
                    "Esta cuenta de usuario ha sido deshabilitada."
                "ERROR_TOO_MANY_REQUESTS", "too-many-requests" ->
                    "Demasiados intentos fallidos. Por favor, reintenta más tarde."
                else -> parseMessage(message)
            }
        }

        if (throwable is FirebaseFirestoreException) {
            return when (throwable.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE -> "El servicio no está disponible en este momento. Revisa tu conexión."
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> "No tienes permisos para realizar esta acción."
                else -> "Error en el servidor de base de datos."
            }
        }

        return parseMessage(message)
    }

    private fun parseMessage(message: String): String {
        return when {
            message.contains("supplied auth credential is incorrect") ||
            message.contains("credential") ||
            message.contains("password is invalid") ||
            message.contains("user not found") ->
                "Correo o contraseña incorrectos."
            message.contains("badly formatted") || message.contains("email address is badly") ->
                "El formato del correo electrónico no es válido."
            message.contains("already in use") || message.contains("already exists") ->
                "El correo electrónico ya se encuentra registrado."
            message.contains("weak password") ->
                "La contraseña es demasiado débil."
            message.contains("canceled") || message.contains("cancelled") ->
                "Operación cancelada por el usuario."
            message.contains("network") || message.contains("connection") ->
                "Error de conexión. Verifica tu conexión a internet."
            else -> "Ocurrió un error al procesar la solicitud."
        }
    }
}
