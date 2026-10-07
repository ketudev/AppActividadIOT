# Especificación: Módulo de Control de Luces de Dormitorio IoT

## Objetivo
Implementar la gestión remota del apagado y control de luces de dormitorio, cumpliendo con la rúbrica de la Evaluación Sumativa N°2. Incluye guardado de usuarios en Firestore, preferencias del negocio (Switch/Spinner), lista en tiempo real y operaciones CRUD (Crear, Leer, Editar, Eliminar) sobre la colección `luces_dormitorio`.

## Arquitectura y Diseño

### Patrón: MVVM (Model-View-ViewModel)

### Estructura de Paquetes
```
com.ketudev.appactividadiot/
├── data/
│   └── services/
│       ├── AuthService.kt              # Firebase Auth + Guardado en Firestore 'usuarios'
│       └── LucesService.kt             # Operaciones CRUD sobre Firestore 'luces_dormitorio'
├── features/
│   ├── auth/                           # LoginActivity, RegisterActivity, AuthViewModel
│   ├── main/
│   │   ├── MainActivity.kt             # Pantalla de bienvenida + Preferencias (Switch / Spinner)
│   │   └── MainViewModel.kt            # ViewModel para preferencias y usuario
│   └── luces/
│       ├── LucesActivity.kt            # Pantalla principal del CRUD (Lista + FAB)
│       ├── LucesViewModel.kt           # ViewModel del CRUD en tiempo real
│       └── LucesAdapter.kt             # RecyclerView Adapter para la lista de luces
├── models/
│   ├── User.kt                         # Modelo de usuario para Firestore
│   ├── LuzDormitorio.kt                # Modelo de luz con @DocumentId y campo numérico
│   └── AuthResult.kt
└── utils/
    └── ValidationUtils.kt
```

### Clases e Interfaces

| Clase | Responsabilidad |
|---|---|
| `LuzDormitorio` | Modelo de datos con `@DocumentId val id: String`, `habitacion: String`, `consumoWatts: Int` (numérico), `estado: String`. |
| `User` | Modelo de usuario en Firestore `{ id, nombre, email }`. |
| `LucesService` | Escucha la colección `luces_dormitorio` con `addSnapshotListener`, agrega, edita y elimina documentos. |
| `LucesViewModel` | Expone LiveData con la lista de luces en tiempo real y estados de carga/error del CRUD. |
| `LucesAdapter` | Adaptador para el RecyclerView. Cada item rotulado: `"Dormitorio Principal - 60 W [Encendida]"`. |

## UI / UX

### MainActivity (`activity_main.xml`)
- Saludo personalizado: `"¡Bienvenido, {Nombre}!"`
- Descripción del servicio: `"Sistema de Apagado Remoto de Luz de Dormitorio"`
- **Sección de Preferencias:**
  - `MaterialSwitch`: `"Apagado automático nocturno"`
  - `Spinner`: Selección de ambiente (`"Dormitorio Principal"`, `"Dormitorio Infantil"`, `"Dormitorio Visitas"`)
- **Botón:** `"Ver Dispositivos / Gestionar Luces"` (Navega a `LucesActivity`)

### LucesActivity (`activity_luces.xml`)
- Toolbar con botón de regreso
- `RecyclerView` que muestra la lista de luces en tiempo real
- `FloatingActionButton` (+) para agregar nuevo dispositivo/registro
- Diálogo de formulario para Crear / Editar:
  - Campo Habitación (TextInputLayout)
  - Campo Consumo Watts (TextInputLayout number)
  - Campo Estado (AutoCompleteTextView / Spinner: Encendida / Apagada)
- Diálogo de confirmación (AlertDialog) al Eliminar.

## Flujos de Datos / Casos de Uso

### Flujo 1: Registro de Usuario con guardar en Firestore
1. Usuario se registra en `RegisterActivity` con Nombre, Email, Contraseña.
2. `AuthService` crea la cuenta en Firebase Auth.
3. Inmediatamente guarda un documento en la colección `usuarios` con `{ id, nombre, email }`.
4. Navega a `MainActivity`.

### Flujo 2: Lectura en Tiempo Real (R)
1. Al abrir `LucesActivity`, `LucesViewModel` activa `addSnapshotListener` en Firestore sobre `luces_dormitorio`.
2. Cada cambio en la base de datos actualiza automáticamente la lista del `RecyclerView` sin recargar la pantalla.

### Flujo 3: Crear Registro (C)
1. Usuario toca el botón (+).
2. Se abre un diálogo con el formulario.
3. Si hay campos vacíos, se muestra mensaje de error.
4. Al pulsar Guardar, se escribe en la colección `luces_dormitorio` de Firestore.

### Flujo 4: Editar Registro (U)
1. Usuario toca un elemento de la lista.
2. Se abre el diálogo pre-llenado con los datos del registro.
3. Usuario modifica el consumo (watts) o estado y presiona Guardar.
4. Se actualiza el documento en Firestore con el mismo `id`.

### Flujo 5: Eliminar Registro (D)
1. Usuario toca el botón de eliminar (icono de basurero) en el elemento.
2. Aparece un `AlertDialog` de confirmación: *"¿Deseas eliminar este dispositivo?"*.
3. Al confirmar, se elimina el documento en Firestore.

## Casos de Prueba (Guion de la Rúbrica)

1. **Prueba 1:** Compilado e icono/nombre de app propio.
2. **Prueba 2:** Validaciones en Registro (email sin dominio, pass corta).
3. **Prueba 3:** Registro correcto crea usuario en Auth y documento en la colección `usuarios`.
4. **Prueba 4-6:** Validaciones de Login y pantalla de Bienvenida.
5. **Prueba 7:** Preferencias con Switch y Spinner del negocio.
6. **Prueba 8:** Lista muestra al menos 5 registros rotulados con lenguaje de negocio desde `luces_dormitorio`.
7. **Prueba 9:** Crear con campos vacíos bloqueado.
8. **Prueba 10:** Crear registro completo actualizado al instante.
9. **Prueba 11:** Editar registro (cambio de valor numérico) actualizado al instante.
10. **Prueba 12:** Eliminar registro con confirmación previa.
