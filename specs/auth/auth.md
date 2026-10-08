# Especificación: Módulo de Autenticación (Auth)

## Objetivo
Proveer un flujo de autenticación completo para la aplicación **Spark** que permita a los usuarios iniciar sesión y registrarse mediante **email/contraseña** y **Google Sign-In**, con identidad visual unificada (`logoGemini.png`), mensajes de error sanitizados en español y validación estricta de entradas.

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
│       ├── LoginActivity.kt            # Vista de Login con logo y título Spark
│       ├── RegisterActivity.kt         # Vista de Registro
│       └── AuthViewModel.kt            # ViewModel compartido
├── models/
│   └── AuthResult.kt                   # Sealed class para resultados
├── utils/
│   ├── ValidationUtils.kt             # Validación de email, password, nombre
│   └── ErrorSanitizer.kt              # Sanitización de errores Firebase a mensajes amigables
└── MainActivity.kt                     # Pantalla principal (post-login)
```

### Clases Involucradas

| Clase | Responsabilidad |
|---|---|
| `AuthService` | Encapsula las llamadas a Firebase Auth (login email, registro, Google Sign-In, logout) |
| `AuthViewModel` | Expone LiveData con estados de UI, coordina validaciones y llamadas al AuthService sanitizando excepciones |
| `AuthResult` | Sealed class: `Idle`, `Loading`, `Success`, `Error(message)` |
| `ValidationUtils` | Funciones de validación para email, contraseña y nombre |
| `ErrorSanitizer` | Convierte excepciones técnicas de Firebase Auth / Red en mensajes comprensibles en español |
| `LoginActivity` | UI de login con logo `logo_gemini.png` (110dp x 110dp), marca "Spark", campos email/password, Google Sign-In |
| `RegisterActivity` | UI de registro con campos nombre, email, password, confirmar password |

## UI / UX

### LoginActivity (`activity_login.xml`)
- **Logo Visual**: Imagen `logo_gemini.png` (110dp x 110dp) centrada en la parte superior.
- **Título**: "Spark" (MaterialTextView en HeadlineLarge bold).
- **Subtítulo**: "Inicia sesión para continuar".
- **TextInputLayout + TextInputEditText** para email (validación de formato y longitud).
- **TextInputLayout + TextInputEditText** para contraseña (con toggle de visibilidad).
- **MaterialButton** "Iniciar Sesión" (filled primary).
- **Divider** "o".
- **MaterialButton** "Continuar con Google" con icono de Google.
- **TextButton** "¿No tienes cuenta? Regístrate" → navega a RegisterActivity.
- **LinearProgressIndicator** en estado Loading.
- **Snackbar** para errores sanitizados globales.

### RegisterActivity (`activity_register.xml`)
- **Toolbar** con botón de retroceso.
- **Título** "Crear cuenta".
- **TextInputLayout + TextInputEditText** para nombre completo.
- **TextInputLayout + TextInputEditText** para email.
- **TextInputLayout + TextInputEditText** para contraseña (mínimo 6 caracteres).
- **TextInputLayout + TextInputEditText** para confirmar contraseña.
- **MaterialButton** "Crear cuenta".
- **ProgressIndicator** en estado Loading.
- **Snackbar** para errores sanitizados.

### Estados de la Vista
1. **Idle**: Formulario habilitado, sin cargando.
2. **Loading**: Formulario deshabilitado, ProgressIndicator visible.
3. **Success**: Navega a MainActivity.
4. **Error**: Muestra Snackbar con mensaje sanitizado en español, restaura formulario.

## Flujos de Datos / Casos de Uso

### Flujo 1: Login con Email/Contraseña
1. Usuario ingresa email y contraseña.
2. ViewModel valida formato de email y contraseña en tiempo real/al enviar.
3. Si hay errores → muestra errores inline en `TextInputLayout`.
4. Si es válido → `AuthService.signInWithEmail(email, password)`.
5. Si ocurre excepción de Firebase → `ErrorSanitizer.sanitize(e)` convierte a mensaje amigable ("Correo o contraseña incorrectos", etc.).
6. Success → navega a `MainActivity`.

### Flujo 2: Google Sign-In
1. Usuario pulsa "Continuar con Google".
2. Se lanza CredentialManager API.
3. Si se cancela o falla red → `ErrorSanitizer` traduce el fallo a mensaje amigable.
4. Success → navega a `MainActivity`.

## Validaciones y Bloqueos Lógicos

| Campo | Reglas / Bloqueos | Mensaje de error |
|---|---|---|
| Email | No vacío + formato email válido + máx 100 caracteres | "Ingresa un correo electrónico válido" |
| Contraseña | No vacía + mínimo 6 caracteres + máx 128 caracteres | "La contraseña debe tener al menos 6 caracteres" |
| Confirmar contraseña | Coincide con contraseña | "Las contraseñas no coinciden" |
| Nombre | No vacío + mínimo 2 caracteres + máx 50 caracteres | "Ingresa tu nombre completo" |

## Mensajes de Error Sanitizados

| Excepción / Error Firebase | Mensaje Sanitizado Presentado al Usuario |
|---|---|
| Credenciales inválidas / contraseña incorrecta / usuario no encontrado | "Correo o contraseña incorrectos." |
| Formato de email malformado | "El formato del correo electrónico no es válido." |
| Correo ya registrado | "El correo electrónico ya se encuentra registrado." |
| Contraseña débil | "La contraseña es demasiado débil. Usa al menos 6 caracteres." |
| Sin conexión a internet | "Error de conexión. Verifica tu conexión a internet." |
| Demasiados intentos fallidos | "Demasiados intentos fallidos. Intenta más tarde." |
| Cancelación de Google Sign-In | "Inicio de sesión con Google cancelado." |
| Error desconocido | "Ocurrió un error al procesar la solicitud." |

## Casos de Prueba Sugeridos

1. **Branding**: Verificar que el logo y el texto "Spark" aparecen correctamente centrados en LoginActivity.
2. **Icono de la app**: Verificar que el icono de la aplicación en Android tiene fondo blanco con el logo Gemini.
3. **Login fallido con credenciales falsas**: Confirmar que se muestra "Correo o contraseña incorrectos." en lugar del texto crudo de Firebase.
4. **Validación email sin formato**: Muestra error inline "Ingresa un correo electrónico válido".
5. **Validación password corta**: Muestra error inline "La contraseña debe tener al menos 6 caracteres".
6. **Google Sign-In cancelado**: Muestra mensaje sanitizado sin romper la aplicación.
