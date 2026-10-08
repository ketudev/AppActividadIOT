# Especificación: Módulo de Control de Luces de Dormitorio IoT

## Objetivo
Implementar la gestión remota del apagado y control de luces de dormitorio para la aplicación **Spark**, cumpliendo con la rúbrica de la Evaluación Sumativa N°2. Incluye guardado de usuarios en Firestore, preferencias del negocio (Switch/Spinner), lista en tiempo real, validaciones lógicas estrictas (ampolleta/luz máximo 150W, habitación no vacía) y operaciones CRUD (Crear, Leer, Editar, Eliminar) sobre la colección `luces_dormitorio`.

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
│       ├── LucesActivity.kt            # Pantalla principal del CRUD (Lista + FAB + Dialog con validaciones inline)
│       ├── LucesViewModel.kt           # ViewModel del CRUD en tiempo real
│       └── LucesAdapter.kt             # RecyclerView Adapter para la lista de luces
├── models/
│   ├── User.kt                         # Modelo de usuario para Firestore
│   ├── LuzDormitorio.kt                # Modelo de luz con @DocumentId y campo numérico
│   └── AuthResult.kt
└── utils/
    ├── ValidationUtils.kt             # Validaciones lógicas (Watts 1-150W, Habitación)
    └── ErrorSanitizer.kt              # Sanitización de errores Firebase a español amigable
```

### Clases e Interfaces

| Clase | Responsabilidad |
|---|---|
| `LuzDormitorio` | Modelo de datos con `@DocumentId val id: String`, `habitacion: String`, `consumoWatts: Int` (numérico), `estado: String`. |
| `User` | Modelo de usuario en Firestore `{ id, nombre, email }`. |
| `LucesService` | Escucha la colección `luces_dormitorio` con `addSnapshotListener`, agrega, edita y elimina documentos. |
| `LucesViewModel` | Expone LiveData con la lista de luces en tiempo real y estados de carga/error sanitizados del CRUD. |
| `LucesAdapter` | Adaptador para el RecyclerView. Cada item rotulado: `"Dormitorio Principal - 60 W [Encendida]"`. |
| `ValidationUtils` | Contiene bloqueos lógicos: Habitación obligatoria (2-100 chars), Consumo Watts entero entre 1W y 150W max. |

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
- Diálogo de formulario para Crear / Editar (`dialog_luz.xml`) con validación inline previa a guardar:
  - Campo Habitación (TextInputLayout, `etHabitacion`): Texto no vacío, min 2 chars.
  - Campo Consumo Watts (TextInputLayout `etConsumoWatts`): Número entre 1W y 150W máximo.
  - Campo Estado (ExposedDropdownMenu `actvEstado`): Encendida / Apagada.
- Diálogo de confirmación (AlertDialog) al Eliminar.

## Validaciones y Bloqueos Lógicos en Inputs

| Campo | Regla / Límite Lógico | Mensaje de error Inline |
|---|---|---|
| Habitación | Obligatorio, 2 a 100 caracteres | "Ingresa el nombre de la habitación" / "El nombre debe tener al menos 2 caracteres" |
| Consumo Watts | Obligatorio, entero > 0 y ≤ 150 W | "El consumo debe ser mayor a 0 W" / "El consumo de la ampolleta no puede superar los 150 W" |
| Estado | Selección entre "Encendida" y "Apagada" | "Selecciona un estado válido" |

## Flujos de Datos / Casos de Uso

### Flujo 1: Crear Registro con Validación Inline (C)
1. Usuario toca el botón (+).
2. Se abre el diálogo con el formulario.
3. Al pulsar Guardar, la Vista valida los inputs con `ValidationUtils`.
4. Si `consumoWatts > 150` o es `<= 0` → Muestra error inline en `tilConsumoWatts` sin cerrar el diálogo.
5. Si `habitacion` es vacía → Muestra error inline en `tilHabitacion`.
6. Si pasa las validaciones → Se envía a `LucesViewModel.addLuz` y se cierra el diálogo.

### Flujo 2: Editar Registro (U)
1. Usuario toca un elemento de la lista.
2. Se abre el diálogo pre-llenado.
3. Se aplican exactamente las mismas validaciones lógicas (≤ 150W, habitación no vacía).
4. Al validar correctamente, se actualiza en Firestore.

### Flujo 3: Manejo Sanitizado de Errores
1. Cualquier fallo de Firestore o red se procesa con `ErrorSanitizer`.
2. Se presenta una notificación limpia al usuario en lugar de tracebacks técnicos o mensajes en inglés.

## Casos de Prueba (Guion de la Rúbrica)

1. **Prueba 1:** Compilado e icono de app con fondo blanco y logo Gemini.
2. **Prueba 2:** Validaciones en Registro (email sin dominio, pass corta, confirmación).
3. **Prueba 3:** Intento de ingresar foco/luz de > 150W → bloqueado con mensaje inline "El consumo de la ampolleta no puede superar los 150 W".
4. **Prueba 4:** Intento de ingresar consumo <= 0W → bloqueado con error "El consumo debe ser mayor a 0 W".
5. **Prueba 5:** Habitación vacía → bloqueada con error inline.
6. **Prueba 6:** Errores de login (ej. contraseña o correo incorrectos) despliegan mensaje sanitizado en español "Correo o contraseña incorrectos."
7. **Prueba 7:** Crear, Editar y Eliminar luces en tiempo real funcionando correctamente.
