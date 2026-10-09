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
| `ValidationUtils` | Contiene bloqueos lógicos: Habitación obligatoria (2-100 chars), Consumo Watts entero entre 1W y 150W max. Además `esFormularioVacio()` y `validateLuzForm()` para permitir el registro vacío. |

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
  - Texto de ayuda (`tlHelper`): aclara que se puede dejar todo en blanco.
- Diálogo de confirmación (AlertDialog) al Eliminar.

## Validaciones y Bloqueos Lógicos en Inputs

### Regla general: registro vacío permitido

El formulario de creación/edición admite **guardar el registro completamente vacío**
(placeholder para completar más tarde). La regla aplicada es:

- Si **los tres campos están vacíos** → se permite guardar sin error. Se persiste con
  `habitacion = ""`, `consumoWatts = 0`, `estado = ""`.
- Si **al menos un campo tiene contenido** → se aplican las validaciones lógicas
  originales campo a campo, sin excepciones.

De esta forma la rúbrica de "registro vacío" se cumple sin perder los bloqueos
lógicos (150 W máximo, watts > 0, habitación obligatoria) cuando el usuario
sí está escribiendo datos.

Implementación: `ValidationUtils.esFormularioVacio()` y
`ValidationUtils.validateLuzForm()` que devuelve un `LuzFormErrors` con
`esValido` y `primerError`. Es la única puerta de entrada usada por
`LucesActivity` y `LucesViewModel`, evitando validación divergente entre capas.

| Campo | Regla / Límite Lógico | Mensaje de error Inline |
|---|---|---|
| Habitación | Obligatorio (si el formulario tiene contenido), 2 a 100 caracteres | "Ingresa el nombre de la habitación" / "El nombre debe tener al menos 2 caracteres" |
| Consumo Watts | Obligatorio (si el formulario tiene contenido), entero > 0 y ≤ 150 W | "El consumo debe ser mayor a 0 W" / "El consumo de la ampolleta no puede superar los 150 W" |
| Estado | Selección entre "Encendida" y "Apagada" | "Selecciona un estado" / "Selecciona un estado válido" |

### Presentación de registros vacíos

| Situación | Texto mostrado en el item |
|---|---|
| Sin habitación | `"Registro sin completar"` |
| Sin habitación + con estado | `"Sin datos - Estado: {estado}"` |
| Sin habitación + sin estado | `"Sin datos - Sin estado"` |
| Con habitación + watts + sin estado | `"Consumo: {watts} W - Sin estado"` |

- El `FloatingActionButton` / diálogo de alta deja el `ExposedDropdownMenu` de estado
  **sin preseleccionar**, para que "Guardar" sin tocar nada cree el registro vacío.
- El diálogo incluye un texto de ayuda: *"Puedes dejar todo en blanco para crear un
  registro vacío."*
- El toggle (icono de ampolleta) sobre un registro vacío solo alterna el estado,
  sin inventar un consumo (antes habría-forzado 60 W).
- La confirmación de eliminar usa un mensaje alternativo cuando no hay nombre:
  *"¿Deseas eliminar el dispositivo sin nombre?"*

## Flujos de Datos / Casos de Uso

### Flujo 1: Crear Registro con Validación Inline (C)
1. Usuario toca el botón (+).
2. Se abre el diálogo con el formulario (estado sin preseleccionar).
3. Al pulsar Guardar, la Vista valida los inputs con `ValidationUtils.validateLuzForm`.
4. Si **todo está vacío** → se crea el registro vacío (sin errores) y se cierra el diálogo.
5. Si `consumoWatts > 150` o es `<= 0` → Muestra error inline en `tilConsumoWatts` sin cerrar el diálogo.
6. Si `habitacion` es vacía (pero otros campos tienen contenido) → Muestra error inline en `tilHabitacion`.
7. Si pasa las validaciones → Se envía a `LucesViewModel.addLuz` y se cierra el diálogo.

`LucesViewModel.addLuz` vuelve a validar con la misma función como segunda barrera
(defensa en profundidad ante llamadas que no pasen por la Vista).

### Flujo 2: Editar Registro (U)
1. Usuario toca un elemento de la lista.
2. Se abre el diálogo pre-llenado (un registro vacío abre los campos en blanco).
3. Se aplican exactamente las mismas validaciones lógicas (≤ 150W, watts > 0,
   habitación no vacía cuando hay contenido). Editar un registro vacío a vacío es válido.
4. Al validar correctamente, se actualiza en Firestore.

### Flujo 2b: Completar un registro vacío
1. El registro vacío aparece en la lista con el rótulo "Registro sin completar".
2. El usuario lo toca y completa los datos.
3. Al guardar, la validación normal se aplica y el registro pasa a estar completo.

### Flujo 3: Manejo Sanitizado de Errores
1. Cualquier fallo de Firestore o red se procesa con `ErrorSanitizer`.
2. Se presenta una notificación limpia al usuario en lugar de tracebacks técnicos o mensajes en inglés.

## Casos de Prueba (Guion de la Rúbrica)

1. **Prueba 1:** Compilado e icono de app con fondo blanco y logo Gemini.
2. **Prueba 2:** Validaciones en Registro (email sin dominio, pass corta, confirmación).
3. **Prueba 3:** Intento de ingresar foco/luz de > 150W → bloqueado con mensaje inline "El consumo de la ampolleta no puede superar los 150 W".
4. **Prueba 4:** Intento de ingresar consumo <= 0W → bloqueado con error "El consumo debe ser mayor a 0 W".
5. **Prueba 5:** Habitación vacía **con watts/estado escritos** → bloqueada con error inline.
6. **Prueba 6:** Formulario de alta completamente en blanco → se crea el registro vacío
   (aparece como "Registro sin completar") y se cierra el diálogo sin errores.
7. **Prueba 7:** Editar el registro vacío y completarlo → pasa a mostrarse con sus datos.
8. **Prueba 8:** Errores de login (ej. contraseña o correo incorrectos) despliegan mensaje sanitizado en español "Correo o contraseña incorrectos."
9. **Prueba 9:** Crear, Editar y Eliminar luces en tiempo real funcionando correctamente.

### Matriz de validación del formulario de luz

| Habitación | Watts | Estado | Resultado |
|---|---|---|---|
| (vacío) | (vacío) | (vacío) | ✅ Registro vacío |
| Sala | 60 | Encendida | ✅ Válido |
| Sala | 200 | Encendida | ❌ Watts > 150 |
| Sala | 0 | Encendida | ❌ Watts ≤ 0 |
| Sala | 60 | (vacío) | ❌ Falta estado |
| (vacío) | 60 | Encendida | ❌ Falta habitación |
| A | 60 | Encendida | ❌ Habitación < 2 chars |
