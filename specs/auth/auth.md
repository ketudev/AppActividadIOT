# Especificación: Módulo de Autenticación (Auth)

## Objetivo
Proveer un flujo de autenticación completo que permita a los usuarios iniciar sesión y registrarse mediante **email/contraseña** y **Google Sign-In**, con validación de entradas y UI basada en Material Design 3.

## Arquitectura y Diseño

### Patrón: MVVM (Model-View-ViewModel)

### Estructura de Paquetes
```
com.ketudev.appactividadiot/
├── data/
│   └── services/
│       └── AuthService.kt              # Wrapper de Firebase Auth
├── features/
│   └── auth/
│       ├── LoginActivity.kt            # Vista de Login
│       ├── RegisterActivity.kt         # Vista de Registro
│       └── AuthViewModel.kt            # ViewModel compartido
├── models/
│   └── AuthResult.kt                   # Sealed class para resultados
├── utils/
│   └── ValidationUtils.kt             # Validación de email/password
└── MainActivity.kt                     # Pantalla principal (post-login)
```

### Clases Involucradas

| Clase | Responsabilidad |
|---|---|
| `AuthService` | Encapsula las llamadas a Firebase Auth (login email, registro, Google Sign-In, logout) |
| `AuthViewModel` | Expone LiveData con estados de UI, coordina validaciones y llamadas al AuthService |
| `AuthResult` | Sealed class: `Loading`, `Success`, `Error(message)` |
| `ValidationUtils` | Funciones de validación para email y contraseña |
| `LoginActivity` | UI de login con campos email/password, toggle password, botón Google Sign-In |
| `RegisterActivity` | UI de registro con campos nombre, email, password, confirmar password |

## UI / UX

### LoginActivity (`activity_login.xml`)
- **Logo/Título** de la app centrado en la parte superior
- **TextInputLayout + TextInputEditText** para email (validación formato email)
- **TextInputLayout + TextInputEditText** para contraseña (con `endIconMode="password_toggle"`)
- **MaterialButton** "Iniciar Sesión" (primary, filled)
- **Divider** con texto "O continuar con"
- **MaterialButton** con icono de Google "Continuar con Google" (outlined/tonal)
- **TextButton** "¿No tienes cuenta? Regístrate" → navega a RegisterActivity
- **ProgressIndicator** circular en estado Loading
- **Snackbar** para errores

### RegisterActivity (`activity_register.xml`)
- **Toolbar** con botón de retroceso
- **Título** "Crear cuenta"
- **TextInputLayout + TextInputEditText** para nombre completo
- **TextInputLayout + TextInputEditText** para email
- **TextInputLayout + TextInputEditText** para contraseña (con toggle y requisitos visibles)
- **TextInputLayout + TextInputEditText** para confirmar contraseña (con toggle)
- **MaterialButton** "Crear cuenta" (primary, filled)
- **ProgressIndicator** circular en estado Loading
- **Snackbar** para errores

### Estados de la Vista
1. **Idle**: Formulario habilitado, sin indicadores
2. **Loading**: Formulario deshabilitado, ProgressIndicator visible
3. **Success**: Navega a MainActivity
4. **Error**: Muestra Snackbar con mensaje de error, formulario habilitado

## Flujos de Datos / Casos de Uso

### Flujo 1: Login con Email/Contraseña
1. Usuario ingresa email y contraseña
2. Al pulsar "Iniciar Sesión", ViewModel valida los campos
3. Si la validación falla → muestra errores inline en los TextInputLayouts
4. Si pasa → AuthService.signInWithEmail(email, password) vía Firebase Auth
5. Resultado Success → navega a MainActivity (con FLAG_CLEAR_TASK)
6. Resultado Error → muestra Snackbar con el mensaje

### Flujo 2: Google Sign-In
1. Usuario pulsa "Continuar con Google"
2. Se lanza el flujo de CredentialManager (nueva API)
3. Se obtiene el GoogleIdToken
4. AuthService.signInWithGoogle(idToken) → Firebase Auth con credencial
5. Success → navega a MainActivity
6. Error → Snackbar

### Flujo 3: Registro con Email/Contraseña
1. Usuario completa nombre, email, password, confirmar password
2. Validaciones: nombre no vacío, email válido, password ≥ 6 chars, passwords coinciden
3. AuthService.createAccount(email, password) → Firebase Auth
4. Post-registro: se actualiza displayName con el nombre
5. Success → navega a MainActivity
6. Error → Snackbar

### Flujo 4: Sesión Persistente
1. Al iniciar LoginActivity, verificar si ya hay un usuario autenticado
2. Si `FirebaseAuth.currentUser != null` → navega directo a MainActivity

## Validaciones

| Campo | Regla | Mensaje de error |
|---|---|---|
| Email | No vacío + formato email válido (Patterns.EMAIL_ADDRESS) | "Ingresa un correo electrónico válido" |
| Contraseña | No vacía + mínimo 6 caracteres | "La contraseña debe tener al menos 6 caracteres" |
| Confirmar contraseña | Coincide con contraseña | "Las contraseñas no coinciden" |
| Nombre | No vacío | "Ingresa tu nombre" |

## Dependencias Necesarias

- `com.google.firebase:firebase-bom` (Firebase BOM)
- `com.google.firebase:firebase-auth-ktx` (Firebase Auth)
- `com.google.android.gms:play-services-auth` (Google Sign-In / CredentialManager)
- `androidx.credentials:credentials` (Credential Manager)
- `androidx.credentials:credentials-play-services-auth` (Credential Manager GMS)
- `com.google.android.libraries.identity.googleid:googleid` (Google ID)
- `androidx.lifecycle:lifecycle-viewmodel-ktx` (ViewModel)
- `androidx.lifecycle:lifecycle-livedata-ktx` (LiveData)
- Plugin: `com.google.gms.google-services`

## Casos de Prueba Sugeridos

1. **Login exitoso** con email/contraseña válidos
2. **Login fallido** con credenciales incorrectas → muestra error
3. **Validación email vacío** → muestra error inline
4. **Validación email inválido** → muestra error inline
5. **Validación contraseña corta** (< 6 chars) → muestra error inline
6. **Toggle visibilidad** de contraseña funciona correctamente
7. **Google Sign-In exitoso** → navega a MainActivity
8. **Google Sign-In cancelado** → no hace nada / muestra mensaje
9. **Registro exitoso** → navega a MainActivity con displayName actualizado
10. **Registro con passwords no coincidentes** → muestra error
11. **Sesión persistente**: al reabrir la app, va directo a MainActivity
12. **Estado Loading** deshabilita formulario y muestra indicador
13. **Navegación Login ↔ Registro** funciona correctamente
