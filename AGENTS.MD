# Instrucciones para Agentes de IA

Siempre que el usuario solicite la creación o modificación de componentes (vistas, modelos, servicios, controladores, etc.), debes seguir las siguientes directrices:

1. **Especificaciones de Componentes (Specs):**
   - Antes de escribir código para cualquier componente, o como parte de su implementación, debes crear un archivo de especificación técnica (Spec).
   - La estructura de carpetas obligatoria para almacenar las especificaciones es:
     `specs/<nombre_del_componente>/<nombre_del_componente>.md`
   - Ejemplo: Para un componente de Login, el archivo debe ser `specs/login/login.md`.

2. **Formato de la Especificación (Spec):**
   Cada archivo Markdown de especificación debe incluir:
   - **Objetivo:** Propósito del componente.
   - **Arquitectura y Diseño:** Patrón utilizado (ej. MVVM), ubicación de archivos, clases involucradas.
   - **UI / UX (Si aplica):** Elementos visuales, estados de la vista (Loading, Success, Error).
   - **Flujos de Datos / Casos de Uso:** Descripción detallada del flujo, llamadas de red o bases de datos (Firebase).
   - **Casos de Prueba sugeridos:** Lista de escenarios a validar.

3. **Mantenimiento:**
   - Si se cambia la lógica o estructura de un componente, la especificación en `specs/` debe ser actualizada para mantener consistencia con el código base.
