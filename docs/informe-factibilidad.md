# Informe de Factibilidad
## App móvil para el control remoto de la iluminación de habitaciones con microcontrolador (Arduino / ESP32)

**Proyecto:** Sistema de Control de Luminaria de Habitaciones (IoT)
**Tipo de informe:** Factibilidad técnica (y complementaria: económica, operativa, legal y temporal)
**Versión:** 1.0

---

## 1. Resumen ejecutivo

El proyecto consiste en desarrollar una **aplicación móvil** que permita encender, apagar y regular la iluminación de una o varias habitaciones de una vivienda, comunicándose con un **microcontrolador** (Arduino o ESP32) instalado junto a la instalación eléctrica de las luminarias.

La factibilidad global del proyecto es **ALTA**. Los componentes necesarios (hardware de bajo costo, protocolos estándar como Wi-Fi y MQTT, plataformas cloud gratuitas o de bajo costo, y herramientas de desarrollo maduras) se encuentran ampliamente disponibles, con documentación abundante y comunidad activa. El mayor riesgo no es técnico, sino de **seguridad y operación estable** (autenticación del canal, autorización por usuario, manejo de fallos de red y seguridad eléctrica de la instalación).

| Dimensión | Evaluación | Comentario |
|---|---|---|
| Factibilidad técnica | **Alta** | Stack maduro: Android + Wi-Fi + MQTT + microcontrolador |
| Factibilidad económica | **Alta** | Costo de prototipo entre USD 30 y 90; infraestructura casi gratuita |
| Factibilidad operativa | **Alta** | Curva de aprendizaje baja para el usuario final; mantenimiento mínimo |
| Factibilidad legal | **Media-Alta** | Requiere manejo responsable de datos personales (Ley 19.628 / RGPD) y normativa eléctrica local |
| Factibilidad temporal | **Alta** | Entre 6 y 10 semanas para un MVP completo |

---

## 2. Objetivo del proyecto

### 2.1 Objetivo general
Implementar un sistema domótico de bajo costo que permita controlar la iluminación de las habitaciones de una vivienda desde cualquier teléfono Android, con reflejo en tiempo real del estado de las luminarias.

### 2.2 Objetivos específicos
1. Seleccionar el microcontrolador más adecuado para la aplicación (Arduino vs. ESP32).
2. Definir la arquitectura de comunicación entre la app móvil, la nube y el dispositivo.
3. Verificar la disponibilidad de herramientas, librerías y servicios necesarios.
4. Estimar costos, tiempos y riesgos del proyecto.
5. Concluir sobre la viabilidad de implementación y la ruta recomendada.

### 2.3 Alcance
**Incluido:**
- App Android (Kotlin) con autenticación de usuarios.
- Registro y gestión de dispositivos/habitaciones.
- Envío de comandos: ON / OFF y, opcionalmente, regulación de brillo (PWM / dimmer).
- Recepción del estado real del dispositivo en la app.
- Panel de configuración del dispositivo (Wi-Fi, broker, credenciales).

**Fuera de alcance (fase 1):**
- Control por voz, domótica exterior por geolocalización.
- Integración con Siri/HomeKit/Matter.
- Aplicación iOS.
- Detección de consumo eléctrico real (medición con sensores CT).

---

## 3. Análisis de factibilidad técnica

### 3.1 Disponibilidad de hardware

#### 3.1.1 Comparativa Arduino vs. ESP32

| Criterio | Arduino Uno R3 | ESP32 (ESP32-WROOM-32) |
|---|---|---|
| MCU | ATmega328P (8 bits, 8 MHz) | Xtensa LX6 de 32 bits, doble núcleo, 240 MHz |
| Conectividad | Solo por módulo externo (Wi-Fi Shield / ESP8266) | **Wi-Fi y Bluetooth integrados** |
| Memoria | 2 KB SRAM / 32 KB flash | **520 KB SRAM / 4 MB flash** |
| GPIO PWM | 6 salidas (3 timers) | **Hasta 16 canales PWM de 16 bits** |
| GPIO disponibles | 14 | 34 (con ADC, DAC, SPI, I2C) |
| Entradas analógicas | 6 (10 bits) | 18 (12 bits) + 2 DAC |
| Tensión de operación | 5 V | **3,3 V** (algunos módulos son compatibles con 5 V) |
| Alimentación desde salida | No | **Alimentación por pines de 5 V (VIN)** |
| Librerías MQTT | Requiere apilar | **PubSubClient, ArduinoMqttClient nativas** |
| Costo unitario aprox. | USD 8–12 | USD 3–7 |
| Ideal para | Sensores simples, lectura de datos por cable | **IoT con Wi-Fi, control remoto** |

**Conclusión:** para esta aplicación el **ESP32 es claramente superior y se recomienda como plataforma principal**. El Arduino solo sería aceptable si se agrega explícitamente un módulo Wi-Fi externo, lo que duplica la complejidad y el costo sin aportar ventaja.

#### 3.1.2 Actuadores y relés

Para conmutar la luminaria se requiere un **módulo de relé**:

| Componente | Carga máxima (resistiva) | Uso típico para luminarias LED |
|---|---|---|
| Relé SRD-05VDC-SLA-C | 10 A / 250 V AC | **Recomendado** (hasta ~500 W en LED) |
| Relé 1 canal con optoacoplador | 10 A / 250 V AC | Alternativa equivalente |
| Módulo relé 4 canales (LDLR) | 10 A por canal | Permite controlar hasta 4 habitaciones |

**Consideraciones críticas de seguridad eléctrica (crítico para la factibilidad):**
- Usar **módulos relé con aislamiento galvánico por optoacoplador** y fuente de alimentación separada para la bobina (por ejemplo, optoacoplador tipo PC817).
- **Nunca** alimentar la bobina del relé directamente desde un pin del ESP32 sin transistor de driver (o zócalo con optoacoplador), para evitar dañar el pin.
- Realizar la instalación dentro de una caja eléctrica cerrada, con cableado de red y tierra.
- La instalación debe realizarla un **electricista certificado**; la app no debe permitir manipular directamente la red eléctrica.
- Considerar el uso de **contactores o relés de estado sólido (SSR)** si las luminarias superan los 500 W.

#### 3.1.3 Fuentes de alimentación
- **Fuente conmutada (switching) de 5 V / 1–2 A** para alimentar el ESP32 (evita el uso del puerto USB del teléfono).
- Alternativa: **adaptador de 12 V con regulador LM2596 o buck converter** en la caja eléctrica, si se desea alimentar el relé desde la misma fuente.

### 3.2 Protocolos de comunicación

#### 3.2.1 Opción A — MQTT sobre broker en la nube (**RECOMENDADA**)

- **Protocolo:** MQTT 3.1.1 / 5.0 sobre **TLS 1.2** (puerto 8883).
- **Broker recomendado:** HiveMQ Cloud (plan gratuito para pruebas), EMQX Cloud, Mosquitto autoalojado en servidor propio, o un broker dentro de la red local.
- **Topología:**
  ```
  [App Android] --HTTPS--> [Firebase Auth + Cloud Functions]
                              |
                              v  (inyecta credenciales MQTT temporales)
  [ESP32] <======= MQTT/TLS =======> [Broker en la nube]
     |                                    ^
     |-- ON/OFF -------------------------|
     |-- estado real --------------------|
                                        [App Android] --subscribe--> [Broker]
  ```
- **Ventajas:**
  - Async y ligero: ideal para dispositivos con memoria y ancho de banda limitados.
  - Mecanismo **Last Will and Testament (LWT)**: el broker detecta la caída del dispositivo y publica su estado como `offline`, lo que resuelve el problema del estado "pegado".
  - Retransmisión y QoS 1 (al menos una vez) para mayor confiabilidad.
  - Soporte nativo de "último mensaje retenido" (*retained message*), que permite a la app conocer el estado real al conectarse.
- **Desventajas:** requiere un broker externo (aunque sea gratuito) y un esquema de credenciales por dispositivo.

#### 3.2.2 Opción B — HTTP/REST + Firebase Realtime Database

- La app escribe el comando en un nodo `/dispositivos/{id}/comando`; el ESP32 consulta vía HTTP polling.
- **Ventaja:** simplicidad conceptual, integración directa con Firebase.
- **Desventajas:** *polling* (latencia y consumo), alto costo de energía del ESP32 por lecturas periódicas, y no permite la retención de mensajes ("retained") ni notificaciones instantáneas al dispositivo.

#### 3.2.3 Opción C — Bluetooth / Wi-Fi directo (BLE)

- **Ventaja:** independencia total del internet y menor latencia.
- **Desventaja:** no funciona fuera del rango (típicamente 10–30 m), lo que contradice el objetivo de "control remoto". Se descarta como solución principal; puede evaluarse como modo de **configuración inicial** del dispositivo.

**Conclusión:** la arquitectura con **MQTT/TLS como canal de control** y **Firebase como capa de identidad y datos de negocio** es la más factible y escalable.

### 3.3 Plataforma móvil (Android)

- **Lenguaje:** Kotlin.
- **Arquitectura:** MVVM (View + ViewModel + Repository/Service).
- **Librerías relevantes y su disponibilidad:**

| Necesidad | Librería / Servicio | Disponibilidad |
|---|---|---|
| Autenticación | Firebase Authentication | Estable y gratuita (Spark plan) |
| Base de datos de negocio | Firestore / Realtime Database | Estable, con modo offline |
| Pub/Sub seguro hacia MQTT | Google Cloud Functions para Firebase | Estable |
| UI | AndroidX (AppCompat, ConstraintLayout, Material, RecyclerView, ViewBinding) | Estable |
| Estado reactivo | LiveData / StateFlow + ViewModel | Estable |
| Testing | JUnit, Espresso | Estable |

Todas las dependencias son **gratuitas en la modalidad Spark (Blaze)** y cuentan con documentación oficial. Esto elimina el principal obstáculo económico del proyecto.

### 3.4 Requisitos no funcionales

| Requisito | Meta | Viabilidad |
|---|---|---|
| Latencia comando → confirmación de estado | < 2 s en red local / < 5 s en WAN | Alta |
| Disponibilidad del servicio | ≥ 99 % (dependencia del broker y de Firebase) | Alta con broker gestionado |
| Autonomía con internet caído | Mínimo 1 h con Wi-Fi local + broker local (modo failover) | Media-Alta |
| Escalabilidad | Hasta 50–100 dispositivos por usuario | Alta |
| Consumo del microcontrolador | < 200 mA activo; modo deep-sleep no aplica en control permanente | Alta |
| Seguridad de datos | TLS en tránsito, tokens cortos, cifrado en reposo de Firebase | Alta |
| Seguridad eléctrica | Aislamiento, protección contra cortocircuitos, caja cerrada | Alta (conelectricista) |

---

## 4. Arquitectura propuesta

```
┌─────────────────────────────┐
│   APP ANDROID (Kotlin/MVVM) │
│  - Login / Registro          │
│  - Lista de habitaciones     │
│  - Toggle ON/OFF             │
│  - Slider de brillo (PWM)   │
└───────────┬─────────────────┘
            │ HTTPS
            ▼
┌──────────────────────────────────┐
│  CAPA CLOUD                        │
│  - Firebase Auth (identidad)       │
│  - Firestore (habitaciones,       │
│    permisos, historial)            │
│  - Cloud Function (broker MQTT     │
│    con credenciales por usuario)   │
└───────────┬───────────────────────┘
            │ MQTT over TLS :8883
            ▼
┌──────────────────────────────────┐
│  BROKER (HiveMQ Cloud / EMQX)     │
└───────────┬───────────────────────┘
            │
            ▼
┌──────────────────────────────────┐
│  ESP32 + Módulo Relé              │
│  - Tasmota / firmware propio      │
│  - Suscripción a topics propios   │
│  - Publicación de estado (retain) │
│  - LWT + heartbeat                │
│  - Botón físico local (respaldo)  │
└──────────────────────────────────┘
```

### 4.1 Esquema de topics MQTT

```
smartlight/{usuarioId}/{habitacionId}/set      → payload: "ON" | "OFF" | "0..100"
smartlight/{usuarioId}/{habitacionId}/state    → payload: "ON" | "OFF" (retained)
smartlight/{usuarioId}/{habitacionId}/status   → payload: "online" | "offline" (LWT)
```

### 4.2 Firmware alternativo que simplifica la implementación

El proyecto **Tasmota** (open source) ya implementa MQTT, control de relé, PWM y actualización OTA para ESP32. Utilizarlo reduce drásticamente el esfuerzo de desarrollo del firmware y constituye una decisión de bajo riesgo.

---

## 5. Análisis de factibilidad económica

### 5.1 Costos de hardware (prototipo por habitación)

| Componente | Cantidad | Costo aprox. (USD) |
|---|---|---|
| ESP32 DevKit V1 (30 pines) | 1 | 5–8 |
| Módulo relé 1 canal 5 V con optoacoplador | 1 | 2–3 |
| Fuente conmutada 5 V / 2 A | 1 | 4–6 |
| Cable, bornas, caja eléctrica, fusible | 1 | 6–10 |
| **Total por habitación** | | **≈ 17–27** |

### 5.2 Costos de software y servicios

| Servicio | Plan | Costo |
|---|---|---|
| Firebase Auth + Firestore | Spark | 0 USD |
| Cloud Functions | Solo Spark (1 MB), o Blaze bajo consumo | 0 – bajo |
| Broker MQTT gestionado | HiveMQ Cloud Free / EMQX Serverless | 0 – 10 USD/mes |
| Hosting propio del broker | Raspberry Pi o servidor | 0 USD (inversiónhw única ~ 60 USD) |
| Android Studio | Community | 0 USD |
| **Total mensual estimado** | | **0 – 15 USD** |

### 5.3 Costos de desarrollo
El esfuerzo es del orden de **1 persona × 8 a 12 semanas** para un MVP con funcionalidades de ON/OFF y brillo, o **1 persona × 4 a 6 semanas** si se aprovecha Tasmota y se limita el alcance a ON/OFF.

**Veredicto:** costo total del proyecto (desarrollo + hardware + operación del primer año) en el rango de **USD 200 a 600**, lo que se sitúa muy por debajo del costo de una solución comercial equivalente.

---

## 6. Factibilidad operativa

- **Curva de aprendizaje del usuario:** baja; la interacción es un botón por habitación.
- **Mantenimiento:** bajo; los dispositivos usan firmware estándar y pueden actualizarse por OTA.
- **Dependencias externas:** Firebase y el broker. Ambas cuentan con planes gratuitos y con SLA de servicio; existe riesgo de cambios de precio (mitigable autoalojando el broker).
- **Modo de respaldo:** cada habitación debe incluir un **interruptor físico manual en paralelo** con el relé, para que el sistema nunca deje la habitación sin iluminación si falla la red, el broker o el firmware.
- **Soporte:** documentación sencilla + video corto de instalación y uso.

---

## 7. Factibilidad legal y de cumplimiento

| Aspecto | Requisito | Cumplimiento propuesto |
|---|---|---|
| Datos personales | Nombre, correo electrónico, tokens de acceso | Consentimiento informado, política de privacidad, cifrado en tránsito y reposo, derecho de eliminación de cuenta |
|KERAM regulations | Ley 19.628 (Chile) o RGPD (UE) | Registro de consentimiento y eliminación bajo solicitud |
| Seguridad eléctrica | Normativa local de instalaciones eléctricas | Instalación por profesional habilitado, uso de elementos homologados |
| Certificación de producto | Depende del país y del uso comercial | En fase académica no aplica; para distribución comercial, evaluar certificación del módulo y del producto |
| Licencias de software | Dependencias open source | MIT/Apache mayormente; Tasmota GPLv3 (compatible con uso no propietario) |

---

## 8. Factibilidad temporal (cronograma estimado)

| Fase | Duración | Entregables |
|---|---|---|
| 1. Investigación y definición de arquitectura | 1 semana | Este informe, diagramas, decisión de componentes |
| 2. Prototipo de hardware | 1,5 semanas | ESP32 + relé + control manual por MQTT/serial |
| 3. Prototipo de firmware | 1 semana | Suscripción/publicación MQTT, LWT, retained state, OTA |
| 4. Base de datos y autenticación | 1 semana | Firebase Auth, Firestore, reglas de seguridad |
| 5. App Android (auth + CRUD de habitaciones) | 2 semanas | Login, registro, lista, asociación dispositivo-habitación |
| 6. App Android (control ON/OFF y brillo) | 1,5 semanas | Toggle, slider, indicadores de estado en vivo |
| 7. Integración, pruebas y hardening | 1,5 semanas | Pruebas end-to-end, manejo de errores, validación eléctrica |
| 8. Documentación y demostración | 0,5 semana | Manual, video, presentación |
| **Total** | **≈ 9,5 semanas** | **MVP funcional** |

---

## 9. Riesgos y mitigaciones

| # | Riesgo | Probabilidad | Impacto | Mitigación |
|---|---|---|---|---|
| 1 | **Acceso no autorizado** al control de la vivienda | Media | Alto | MQTT sobre TLS con credenciales únicas por dispositivo; tokens de acceso generados por Cloud Function con expiración; ACL por usuario en el broker; nunca hardcodear contraseñas en el firmware |
| 2 | Pérdida de internet / caída del broker | Media | Medio | Broker local como failover, último mensaje retenido, interruptor físico manual, reintentos con back-off exponencial |
| 3 | **Estado desincronizado** (app dice "encendida" pero no lo está) | Alta | Medio | LWT + heartbeat + retained message; la UI muestra estado "desconocido" si no hay confirmación en N segundos |
| 4 | Daño al ESP32 por transitorios de la red eléctrica | Media | Alto | Optoacoplador en el relé, flyback diode, fuente con protección, caja cerrada, instalación profesional |
| 5 | Duplicidad de IDs o seriales clonados | Baja | Alto | Registro del dispositivo vinculado a la cuenta; serial único quemado en fábrica o generado por secure element |
| 6 | Dependencia de servicios cloud gratuitos | Media | Medio | Arquitectura desacoplada (broker intercambiable), autoalojamiento posible sin cambios en el firmware |
| 7 | Calor/consumo del relé con cargas altas | Media | Medio | Dimensionar relé según la carga (relé de 10 A, SSR si > 500 W), no controlar cargas > 80 % de la capacidad nominal |
| 8 | Alcance excesivo | Alta | Medio | MVP con ON/OFF primero; brillo y funciones avanzadas en iteraciones posteriores |
| 9 | Falta de pruebas en condiciones reales | Media | Medio | Plan de pruebas documentado (ver sección 10) |

---

## 10. Casos de prueba sugeridos para validación de factibilidad

1. **Encendido/apagado individual:** cada habitación se controla de forma independiente.
2. **Comando desde red externa:** control exitoso con la app fuera de la red local (datos móviles).
3. **Estado en vivo:** al abrir la app, el estado mostrado coincide con el estado físico real.
4. **Corte de internet:** al perder la conexión, el sistema no deja la habitación sin luz (interruptor manual operativo).
5. **Caída del broker:** el dispositivo publica `offline` vía LWT en menos de 30 segundos.
6. **Reintento:** si se pierde un comando, la orden se reenvía automáticamente al reconectar.
7. **Usuario no autorizado:** una cuenta distinta a la propietaria no puede publicar comandos en los topics ajenos.
8. **Sobrecarga de watts:** el sistema rechaza o advierte cargas superiores a la capacidad del relé configurado.
9. **Persistencia de sesión:** cerrar y reabrir la app mantiene el estado y la sesión.
10. **Caída del Wi-Fi del dispositivo:** el dispositivo se reconecta solo tras restablecerse la red.

---

## 11. Conclusiones

1. **La factibilidad técnica es ALTA.** Todos los elementos requeridos (hardware, protocolos, plataformas y herramientas) son tecnologías estándar, documentadas y de bajo costo.
2. **El ESP32 es la plataforma recomendada** frente a Arduino, debido a su conectividad Wi-Fi integrada, mayor potencia de cómputo, GPIO suficientes y consumo de recursos compatible con MQTT.
3. **La arquitectura más viable es híbrida:** Firebase para identidad y datos de negocio, y MQTT sobre TLS en un broker en la nube como canal de control hacia el ESP32. Esta combinación es escalable, segura y no exige desarrollo de un protocolo propio.
4. **Tasmota** puede reducir significativamente el tiempo de desarrollo de firmware, concentrando el esfuerzo en la aplicación móvil, que es el entregable principal del proyecto.
5. **El costo económico es bajo:** el hardware de una habitación cuesta entre USD 17 y 27, y los servicios de software son gratuitos o de bajo costo.
6. **Los riesgos identificados son gestionables**, y los más críticos (seguridad y estado desincronizado) cuentan con mitigaciones concretas descritas en la sección 9.
7. **Se recomienda continuar con la implementación**, respetando un enfoque iterativo: MVP con ON/OFF, control y brillo, y posterior escalado a múltiples habitaciones y usuarios.

---

## 12. Anexo — Referencias técnicas

- Documentación oficial de ESP32 (espressif.com) y guía "Getting Started".
- Tasmota Documentation (tasmota.com) — supported devices, MQTT commands.
- MQTT Essentials (HiveMQ) — QoS, retained messages, LWT.
- Firebase Documentation — Authentication, Firestore, Cloud Functions, Security Rules.
- Android Developers — App architecture (MVVM), ViewModel, ViewBinding.
- Normativa eléctrica local aplicable a instalaciones domésticas de baja tensión.

---

*Documento generado como parte de la evaluación del proyecto. Los valores de costo son aproximados y de referencia para una implementación de ámbito académico.*
