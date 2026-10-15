package com.ketudev.appactividadiot.utils

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Origen del fallo de autenticación. Permite mostrar un mensaje coherente con la
 * acción que el usuario realmente ejecutó (correo vs. Google).
 */
enum class AuthSource { EMAIL, GOOGLE }

object ErrorSanitizer {

    /**
     * Traduce una excepción de Firebase/Credential Manager a un mensaje en español.
     *
     * @param source origen de la acción. Con [AuthSource.GOOGLE] los errores de
     *   credenciales NO se reportan como "correo o contraseña incorrectos", porque
     *   en Google Sign-In ese texto oculta el error real de configuración (SHA-1 no
     *   registrado en Firebase, cliente OAuth inválido, etc.).
     */
    fun sanitize(throwable: Throwable?, source: AuthSource = AuthSource.EMAIL): String {
        if (throwable == null) return "Ocurrió un error inesperado."

        val message = throwable.message.orEmpty().lowercase()

        if (throwable is FirebaseNetworkException ||
            message.contains("network error") ||
            message.contains("unable to resolve host")
        ) {
            return "Error de conexión. Verifica tu conexión a internet."
        }

        if (throwable is FirebaseAuthException) {
            return mapAuthError(throwable.errorCode, message, source)
        }

        if (throwable is FirebaseFirestoreException) {
            return when (throwable.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "El servicio no está disponible en este momento. Revisa tu conexión."
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "No tienes permisos para realizar esta acción."
                else -> "Error en el servidor de base de datos."
            }
        }

        return parseMessage(message, source)
    }

    private fun mapAuthError(
        errorCode: String,
        rawMessage: String,
        source: AuthSource
    ): String {
        val code = errorCode.lowercase()

        // --- Errores propios del proveedor de identidad (Google) ---
        if (source == AuthSource.GOOGLE) {
            when (code) {
                "error_invalid_idp_response",
                "invalid-idp-response",
                "error_web_context_canceled" -> return MSG_GOOGLE_CONFIG

                "error_account_exists_with_different_credential",
                "account-exists-with-different-credential" ->
                    return "Ya existe una cuenta con ese correo usando otro método de acceso."

                "error_credential_already_in_use",
                "credential-already-in-use" ->
                    return "Esta cuenta de Google ya está vinculada a otra cuenta de la app."

                "error_credential_mismatch",
                "credential-mismatch" ->
                    return "La cuenta de Google no coincide con la cuenta que iniciaste sesión."
            }
        }

        // --- Errores de configuración compartidos ---
        when (code) {
            "error_invalid_credential" -> return if (source == AuthSource.GOOGLE) {
                MSG_GOOGLE_CONFIG
            } else {
                "Correo o contraseña incorrectos."
            }

            "error_invalid_idp_response",
            "invalid-idp-response" -> return MSG_GOOGLE_CONFIG

            "error_operation_not_allowed",
            "operation-not-allowed" ->
                return "El proveedor de acceso con correo no está habilitado en Firebase."

            "error_invalid_email",
            "invalid-email" ->
                return "El formato del correo electrónico no es válido."

            "error_email_already_in_use",
            "email-already-in-use" ->
                return "El correo electrónico ya se encuentra registrado."

            "error_weak_password",
            "weak-password" ->
                return "La contraseña es demasiado débil. Usa al menos 6 caracteres."

            "error_user_disabled",
            "user-disabled" ->
                return "Esta cuenta de usuario ha sido deshabilitada."

            "error_too_many_requests",
            "too-many-requests" ->
                return "Demasiados intentos fallidos. Por favor, reintenta más tarde."

            "error_network_request_failed",
            "error_internal_error",
            "internal-error" ->
                return "Error interno del servicio de autenticación. Reintenta en unos segundos."

            "error_web_context_canceled" -> return "Operación cancelada por el usuario."
        }

        // ERROR_USER_NOT_FOUND / ERROR_WRONG_PASSWORD: válidos solo para correo.
        when (code) {
            "error_wrong_password", "wrong-password",
            "error_user_not_found", "user-not-found" ->
                if (source == AuthSource.EMAIL) return "Correo o contraseña incorrectos."

            "error_email_not_found", "email-not-found" ->
                if (source == AuthSource.EMAIL) return "El correo electrónico no se encuentra registrado."
        }

        return parseMessage(rawMessage, source)
    }

    private fun parseMessage(rawMessage: String, source: AuthSource): String {
        val message = rawMessage.lowercase()
        return when {
            // Errores del proveedor de identidad: NO son problema de contraseña.
            message.contains("invalid_idp_response") ||
            message.contains("invalid idp response") ||
            message.contains("developer console") ||
            message.contains("api key") ||
            message.contains("oauth") -> MSG_GOOGLE_CONFIG

            message.contains("web_context_canceled") ||
            message.contains("canceled") ||
            message.contains("cancelled") ->
                "Operación cancelada por el usuario."

            message.contains("no credential") ||
            message.contains("nocredentialexception") ->
                "No se encontró una cuenta de Google disponible en este dispositivo."

            message.contains("already in use") || message.contains("already exists") ->
                "El correo electrónico ya se encuentra registrado."

            message.contains("weak password") ->
                "La contraseña es demasiado débil. Usa al menos 6 caracteres."

            message.contains("badly formatted") || message.contains("invalid email") ->
                "El formato del correo electrónico no es válido."

            // Mensajes de credenciales: solo tienen sentido con acceso por correo.
            message.contains("supplied auth credential is incorrect") ||
            message.contains("password is invalid") ||
            message.contains("user not found") ||
            (source == AuthSource.EMAIL && message.contains("credential")) ->
                "Correo o contraseña incorrectos."

            message.contains("network") || message.contains("connection") ->
                "Error de conexión. Verifica tu conexión a internet."

            else -> "Ocurrió un error al procesar la solicitud."
        }
    }

    private const val MSG_GOOGLE_CONFIG =
        "No pudimos validar tu cuenta con Google. Agrega la huella SHA-1 del keystore de " +
            "depuración en Firebase > Authentication > Settings > Authorized domains " +
            "(y en el cliente Android de Google Cloud)."
}