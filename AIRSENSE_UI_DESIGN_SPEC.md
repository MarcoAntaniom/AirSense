# AirSense — Documento Maestro de Especificación de Interfaz de Usuario (UI/UX)
> **Propósito de este documento:** Este archivo contiene el desglose técnico, visual y funcional de la interfaz completa de la aplicación Android **AirSense**, adaptada y optimizada para **cocinas profesionales**. Está estructurado para ser entregado a un modelo de Inteligencia Artificial (IA) o diseñador UI/UX con el fin de rediseñar, modernizar o adaptar los layouts a nuevas tendencias (Bento Grid, Glassmorphism, Material 3 Expressive, etc.) manteniendo compatibilidad absoluta con el código Kotlin, ViewBinding y la arquitectura MVVM existente.

---

## 1. Visión General del Producto y Contexto

* **Nombre de la App:** AirSense
* **Tipo de App:** Monitor ambiental y estación de calidad del aire para **cocinas profesionales** y entornos operacionales IoT.
* **Propósito Operacional:** Monitorear en tiempo real gases de combustión producidos por quemadores, hornos y freidoras (CO₂, CO, CH₂O, gas sin procesar MQ) y calidad general del aire (AQI), previniendo intoxicaciones y garantizando la adecuada extracción y ventilación del área culinaria.
* **Fuente de Datos de Sensores:** Microcontrolador ESP8266 (Wi-Fi) con sensor de temperatura/humedad DHT11 y sensor electroquímico MQ conectado vía Google Firebase Realtime Database.
* **Stack Tecnológico UI:** 
  - Android Nativo (Kotlin)
  - Material Design 3 (`Theme.Material3.DayNight.NoActionBar`)
  - View Binding (todas las vistas se referencian por `binding.<view_id>`)
  - Jetpack Navigation Component (`NavHostFragment`) con `BottomNavigationView`
  - Arquitectura MVVM con `StateFlow` y Corrutinas

---

## 2. Jerarquía Operacional del Dashboard (Regla de Oro)

En el contexto de cocina profesional, la interfaz prioriza la lectura operacional crítica en el siguiente orden secuencial estricto:

1. **Posición 1: Nivel de Gas / Combustión:** Tarjeta superior hero con lectura de CO₂ equivalente (ppm), desglose en tiempo real de CO, CH₂O y señal bruta (Gas Raw), chip de severidad y barra de progreso operacional.
2. **Posición 2: Calidad del Aire (AQI) y Estado de Conexión:** Gauge central circular con valor numérico de AQI (0–300) y chip de estado.
3. **Posición 3: Recomendación Operacional Rápida:** Banner contextual con acciones inmediatas para el personal de cocina (ej. *"Ventilación adecuada"*, *"Activar campana extractora"*, *"Extracción al máximo"*).
4. **Posición 4: Confort Ambiental en Cocina (Bento 2 Columnas):** Tarjetas Bento emparejadas para Temperatura (°C / °F) y Humedad Relativa (%).
5. **Posición 5: Explorador de Sensores:** Métricas desglosadas por dispositivo de hardware.
6. **Posición 6: Ajustes y Configuración:** Gestión de alertas sonoras/visuales, umbrales de peligro y conectividad Firebase.

---

## 3. Estados Semánticos Oficiales y Regla de Conexión Offline

### 3.1. Estados del AQI y Gas (Terminología Literal Obligatoria)
La interfaz utiliza **exclusivamente** los siguientes 4 estados literales:
1. **`Óptimo`** (0 - 50 AQI / CO₂ ≤ 800 ppm): Operación culinaria segura. Color verde esmeralda.
2. **`Moderado`** (51 - 100 AQI / CO₂ 801 - 1200 ppm): Flujo de aire moderado, encender extracción baja/media. Color ámbar cálido.
3. **`Peligroso`** (> 100 AQI / CO₂ > 1200 ppm): Peligro por combustión o humo; activar campana extractora al máximo y revisar quemadores. Color rojo alerta.
4. **`Desconectado`** (Sin telemetría confirmada): Dispositivo fuera de línea o sin señal confirmada de Firebase.

### 3.2. Regla de Integridad de Datos Offline
> **Directriz de Seguridad:** Para evitar que el equipo de cocina opere confiando en datos desactualizados o congelados, cuando el estado sea `Desconectado` (`AirSenseState.Stale`), **bajo ninguna circunstancia se presentarán datos no confirmados como actuales**. Todas las métricas numéricas se enmascaran automáticamente con `"--"`, los chips indican `"Desconectado"` en color neutro y las recomendaciones guían a verificar energía y Wi-Fi en cocina.

---

## 4. Sistema de Diseño y Tokens Visuales

### 4.1. Paleta de Colores

#### Modo Claro (`res/values/colors.xml`)
* **Marca Principal:**
  - Primario: `#0C4B59` (`airsense_primary` - Petróleo profundo / Dark Slate Cyan)
  - Primario Oscuro: `#072F38` (`airsense_primary_dark`)
  - Secundario: `#149B93` (`airsense_secondary` - Verde azulado / Teal)
  - Secundario Claro: `#E0F2F1` (`airsense_secondary_light`)
  - Acento: `#F2A027` (`airsense_accent` - Ámbar cálido)
* **Superficies y Fondos:**
  - Fondo de pantalla: `#F8FAFC` (`bg_screen` - Slate 50 muy limpio)
  - Fondo de tarjetas: `#FFFFFF` (`bg_card` - Blanco puro)
  - Superficie sutil / fondo pills: `#F1F5F9` (`bg_card_subtle`)
  - Borde de tarjetas: `#E2E8F0` (`stroke_card` - 1dp de grosor)
* **Tipografía:**
  - Texto principal: `#0F172A` (`text_primary` - Slate 900)
  - Texto secundario / muted: `#64748B` (`text_secondary` - Slate 500)
* **Semáforo Semántico Operacional:**
  - **Óptimo (0 - 50 AQI):** Texto/Ícono `#10B981` (`aqi_good`), Fondo Chip `#D1FAE5` (`aqi_good_bg`)
  - **Moderado (51 - 100 AQI):** Texto/Ícono `#F59E0B` (`aqi_moderate`), Fondo Chip `#FEF3C7` (`aqi_moderate_bg`)
  - **Peligroso (> 100 AQI):** Texto/Ícono `#EF4444` (`aqi_danger`), Fondo Chip `#FEE2E2` (`aqi_danger_bg`)
* **Acentos de Confort Térmico:**
  - Temperatura: `#F97316` (`temp_accent` - Naranja cocina)
  - Humedad: `#0EA5E9` (`humidity_accent` - Azul cielo)

#### Modo Oscuro (`res/values-night/colors.xml`)
* **Superficies y Fondos:**
  - Fondo de pantalla: `#0F172A` (`bg_screen` - Slate 900)
  - Fondo de tarjetas: `#1E293B` (`bg_card` - Slate 800)
  - Superficie sutil: `#1E293B` (`bg_card_subtle`)
  - Borde y divisores: `#334155` (`stroke_card`)
* **Tipografía:**
  - Texto principal: `#F8FAFC` (`text_primary`)
  - Texto secundario: `#94A3B8` (`text_secondary`)
* **Marca y Semáforo:**
  - Primario: `#4DD0E1` (Cian luminoso para contraste)
  - Secundario: `#26A69A`
  - Acento: `#FFB74D`
  - AQI Óptimo: `#34D399` / Fondo `#064E3B`
  - AQI Moderado: `#FBBF24` / Fondo `#78350F`
  - AQI Peligroso: `#F87171` / Fondo `#7F1D1D`

### 4.2. Reglas de Estilo y Forma
* **Radios de Esquinas (`cornerRadius`):**
  - Tarjetas principales / Bento: `20dp` a `24dp`
  - Tarjetas secundarias: `12dp` a `14dp`
  - Botones y campos de entrada: `12dp`
  - Chips de estado: Redondeo completo tipo píldora (`height="28dp"` a `32dp`)
* **Bordes y Sombras:** En lugar de sombras pesadas (`elevation="0dp"`), se utilizan bordes finos de `1dp` con color `stroke_card` para una estética limpia, higiénica y moderna.

---

## 5. Arquitectura y Navegación de la App

### 5.1. Barra Superior y Contenedor Raíz (`activity_main.xml`)
* **Estructura:**
  1. **Top Bar con Identidad de Marca y Píldora de Telemetría en Vivo:**
     - Ícono de marca con fondo redondeado (`bg_brand_mark.xml`).
     - Título *"AirSense"* (18sp, negrita) y subtítulo operacional *"RESPIRA MEJOR"*.
     - **Píldora En Vivo / Offline:**
       - Contenedor con borde (`bg_live_pill.xml`).
       - `View` id: `dotLiveIndicator`: Punto LED circular (verde esmeralda cuando hay conexión en tiempo real, gris apagado cuando está desconectado).
       - `TextView` id: `tvLiveBadge`: Texto dinámico *"EN VIVO"* / *"OFFLINE"*.
  2. **Contenedor Central (`FragmentContainerView` id: `nav_host_fragment_content_main`):**
     - Gestionado por Jetpack Navigation (`nav_graph.xml`).
  3. **Barra de Navegación Inferior (`BottomNavigationView` id: `bottom_navigation`):**
     - Fondo: `bg_card`
     - Elevación: `8dp`
     - Tintes activos e inactivos definidos por selector (`bottom_nav_color_state.xml`)
     - 4 pestañas obligatorias:
       1. **Inicio** (`navigation_home`) ➔ Ícono `@drawable/ic_home`
       2. **Sensores** (`navigation_sensors`) ➔ Ícono `@drawable/ic_sensors`
       3. **Consejos** (`navigation_recommendations`) ➔ Ícono `@drawable/ic_recommendations`
       4. **Ajustes** (`navigation_settings`) ➔ Ícono `@drawable/ic_settings`

---

## 6. Desglose Pantalla por Pantalla (Layouts, IDs y Componentes)

### Pantalla 0: Onboarding para Cocinas Profesionales (`fragment_onboarding.xml` y `item_onboarding_slide.xml`)
* **Comportamiento:** Se muestra solo en el primer inicio (`is_first_run == true`).
* **Componentes clave:**
  - `ViewPager2` id: `viewPagerOnboarding`: Deslizador horizontal de diapositivas explicativas.
  - Diapositiva (`item_onboarding_slide.xml`):
    - `img_slide_icon`: Imagen ilustrativa (140x140dp)
    - `tv_slide_title`: Título principal (24sp, bold)
    - `tv_slide_description`: Descripción explicativa adaptada a cocina (15sp)
  - `MaterialButton` id: `btn_next_onboarding`: Botón fijo inferior ("Siguiente" / "Comenzar", altura 56dp).
* **Contenido de Diapositivas:**
  1. *Monitoreo en Cocinas Profesionales:* Supervisión en tiempo real de gases de combustión (CO₂, CO, CH₂O) y calidad de aire en el área culinaria.
  2. *Prioridad Operacional & Seguridad:* Distinción inmediata de estados Óptimo, Moderado o Peligroso para gestionar la extracción y proteger al equipo.
  3. *Telemetría Confiable en Vivo:* Enlace persistente con sensores IoT y Firebase sin falsos positivos.

---

### Pantalla 1: Pantalla Principal / Dashboard (`fragment_home.xml`)
* **Propósito:** Supervisión operativa inmediata de gases, AQI, recomendación activa y clima de cocina.
* **Layout Base:** `NestedScrollView` con `LinearLayout` vertical y padding de 16dp.
* **Componentes e IDs requeridos:**
  1. **Cabecera de Ubicación y Estado de Conexión:**
     - `TextView` id: `tvLocation`: Muestra la fuente de datos (ej. *"En vivo (Firebase)"*, *"Conectando..."*, o motivo de desconexión).
     - `ImageButton` id: `btnRefresh`: Botón para forzar actualización inmediata.
  2. **POSICIÓN 1: Nivel de Gas / Combustión (Hero Operacional):**
     - `MaterialCardView` id: `cardGasHero`:
       - `Chip` id: `chipGasStatus`: Píldora de estado de gases (`Normal`, `Precaución`, `Peligro`, `Desconectado`).
       - `TextView` id: `tvGasValue`: Concentración en ppm CO₂ equivalente (36sp, negrita, ej. *"450"* o *"--"*).
       - `TextView` id: `tvGasSub`: Desglose en una línea (*"CO: 0.0 ppm · CH₂O: 0.00 ppm · Raw: 100"*).
       - `LinearProgressIndicator` id: `progressGasLevel`: Barra de nivel de saturación de gas (0 a 2000 ppm) con esquinas redondeadas.
  3. **POSICIÓN 2: Calidad del Aire (AQI Central Gauge):**
     - `MaterialCardView` id: `cardHeroGauge`:
       - `CircularProgressIndicator` id: `aqiProgressIndicator`: Indicador circular de 220dp, trazo de 16dp, rango 0–300 AQI.
       - `TextView` id: `tvAqiValue`: Valor numérico (56sp, bold, ej. *"42"* o *"--"*).
       - `Chip` id: `chipAqiStatus`: Píldora de estado (`Óptimo`, `Moderado`, `Peligroso`, `Desconectado`).
       - `TextView` id: `tvLastUpdated`: Marca temporal de última actualización (*"Actualizado ahora"*).
  4. **POSICIÓN 3: Recomendación Operacional Rápida:**
     - `MaterialCardView` id: `cardQuickRecommendation`:
       - `ImageView` id: `ivRecIcon`: Ícono con tinte dinámico según severidad.
       - `TextView` id: `tvRecTitle`: Instrucción directa (ej. *"Activar campana extractora"*, *"Extracción al máximo"*).
       - `TextView` id: `tvRecSubtitle`: Justificación ambiental rápida.
  5. **POSICIÓN 4: Bento Clima de Cocina (2 Columnas con pesos iguales):**
     - `LinearLayout` id: `bentoClimateRow` (`weightSum="2"`):
       - **Tarjeta Temperatura:**
         - `ImageView` id: `ivTemp`: Ícono naranja en badge circular.
         - `TextView` id: `tvTempValue`: Valor térmico (26sp, bold, ej. *"22.5°C"* o *"--"*).
         - `TextView` id: `tvTempLabel`: Estado (ej. *"Confortable"*, *"Calor elevado"*, *"No disponible"*).
       - **Tarjeta Humedad:**
         - `ImageView` id: `ivHumidity`: Ícono azul en badge circular.
         - `TextView` id: `tvHumidityValue`: Humedad relativa (26sp, bold, ej. *"55%"* o *"--"*).
         - `TextView` id: `tvHumidityLabel`: Estado (ej. *"Humedad ideal"*, *"Humedad alta"*, *"No disponible"*).

---

### Pantalla 2: Explorador de Sensores (`fragment_sensors.xml` e `item_sensor_card.xml`)
* **Propósito:** Lista exhaustiva de sensores físicos del ESP8266 y lecturas desglosadas.
* **Layout Base:** `SwipeRefreshLayout` (id: `swipe_refresh_layout`) con `NestedScrollView`.
* **Componentes e IDs requeridos:**
  1. **Tarjetas Bento de Métricas en Vivo:**
     - **Card AQI:** `tv_sensors_aqi_val`, `progress_sensors_aqi` (`LinearProgressIndicator`).
     - **Card CO2 & Gases:** `tv_sensors_co2_val`, `tv_sensors_gases_sub` (desglosa CO y CH₂O), `progress_sensors_co2`.
     - **Card Temperatura:** `tv_sensors_temp_val`, `progress_sensors_temp`.
     - **Card Humedad:** `tv_sensors_humidity_val`, `progress_sensors_humidity`.
  2. **Grilla de Dispositivos IoT:**
     - `RecyclerView` id: `rv_sensors_list`: Grilla de 2 columnas con tarjetas de hardware.
  3. **Tarjeta de Dispositivo IoT (`item_sensor_card.xml`):**
     - `ImageView` id: `ivSensorIcon`: Ícono del tipo de sensor.
     - `View` id: `viewStatusDot`: Punto LED verde/gris de estado en línea.
     - `TextView` id: `tvSensorMainMetric`: Lectura principal formateada con autosize (ej. "450 ppm", "22.5 °C").
     - `TextView` id: `tvSensorType`: Tipo de sensor o variable (ej. "Gas MQ · CO2").
     - `TextView` id: `tvSensorName`: Nombre amigable (ej. "Estación Cocina (ESP8266)").
     - *Interacción:* Al tocar cualquier tarjeta se abre el modal de detalle del sensor.

---

### Pantalla 3: Modal de Detalle de Sensor (`fragment_sensor_detail.xml`)
* **Propósito:** `BottomSheetDialogFragment` emergente con ficha técnica del sensor seleccionado.
* **Componentes e IDs requeridos:**
  - Píldora de arrastre superior (`drag handle`).
  - `ImageView` id: `img_sensor_icon`: Ícono de hardware en badge circular.
  - `TextView` id: `tv_sensor_title`: Nombre del sensor.
  - `TextView` id: `tv_sensor_id`: Identificador técnico (ej. `sns_02 · ESP8266 IoT`).
  - `Chip` id: `chip_sensor_status`: Estado de enlace (*"En Línea (Firebase)"* o *"Fuera de línea"*).
  - Indicador de salud de hardware y rangos normales de operación.

---

### Pantalla 4: Recomendaciones Ambientales (`fragment_recommendations.xml`)
* **Propósito:** Consejos de ventilación, seguridad de gases y optimización de cocina.
* **Componentes e IDs requeridos:**
  - `MaterialCardView` id: `card_active_recommendation`: Tarjeta destacada prioritaria con fondo verde azulado `#0C4B59`.
    - `TextView` id: `tv_active_recommendation_title`: Título dinámico según nivel de riesgo.
    - `TextView` id: `tv_active_recommendation_desc`: Instrucción operativa específica.
  - Tarjetas temáticas informativas fijas:
    - Ventilación y extracción forzada de humos de cocina.
    - Purificación y filtrado de partículas de grasa.
    - Seguridad en quemadores y prevención de monóxido de carbono (CO).
    - Confort térmico del personal de cocina.

---

### Pantalla 5: Configuración y Ajustes (`fragment_settings.xml`)
* **Propósito:** Preferencias del usuario, alertas sonoras/visuales y enlace Firebase.
* **Componentes e IDs requeridos:**
  1. **Notificaciones y Alertas:**
     - `MaterialSwitch` id: `switch_notifications`: Activar/desactivar alertas de umbral de gas.
     - `MaterialButton` id: `btn_test_notification`: Botón de prueba de canal de notificación.
  2. **Tema Visual:**
     - `MaterialButtonToggleGroup` id: `toggle_group_theme`: Selector de 3 botones (`btn_theme_system`, `btn_theme_light`, `btn_theme_dark`).
  3. **Unidades de Medida:**
     - Selector de temperatura Celsius (°C) / Fahrenheit (°F).
  4. **Permisos del Sistema:**
     - Estado de permisos Bluetooth, Ubicación y Notificaciones con botón `btn_request_all_permissions`.
  5. **Configuración de Firebase:**
     - Entradas de texto para Host de Firebase y Token de autenticación (`auth`), con botón de prueba de conexión directa.
  6. **Umbrales Críticos de Cocina:**
     - Controles para definir límites de alerta de CO2 (PPM peligro y advertencia), temperatura máxima y rango de humedad.

---

## 7. Instrucciones para la IA de Rediseño (Prompt Guide)

Al solicitar a una IA (como v0, ChatGPT, Claude, Midjourney o un agente de diseño) que rediseñe la interfaz de AirSense, entrégale este documento y la siguiente consigna:

> **Consigna de Rediseño:**
> 1. **Mantener IDs de Vistas y Tipos:** Es fundamental que **no elimines ni cambies** los `id` de las vistas que tienen interactividad o ViewBinding (por ejemplo: `tvGasValue`, `tvGasSub`, `chipGasStatus`, `progressGasLevel`, `tvAqiValue`, `aqiProgressIndicator`, `chipAqiStatus`, `tvTempValue`, `tvHumidityValue`, `tvLocation`, `btnRefresh`, `rv_sensors_list`, etc.).
> 2. **Estilo Visual:** Se busca un diseño moderno tipo **Bento Grid** con tarjetas redondeadas (20dp - 24dp), micro-interacciones visuales, excelente jerarquía tipográfica y soporte nativo tanto para Modo Claro como Modo Oscuro.
> 3. **Prioridad Operacional (Cocina Profesional):**
>    - 1° El nivel de gas y combustión (CO₂, CO, CH₂O, Raw).
>    - 2° El valor del AQI y el estado de conexión persistente.
>    - 3° La recomendación operacional inmediata.
>    - 4° La temperatura y humedad en tarjetas Bento emparejadas.
> 4. **Estados Oficiales Obligatorios:** Los únicos 4 estados permitidos para el AQI son `Óptimo`, `Moderado`, `Peligroso` y `Desconectado`. Cuando esté `Desconectado`, nunca muestres datos anteriores como actuales (usa `"--"`).
> 5. **Salida esperada:** Proporcionar los archivos XML actualizados listos para reemplazar en `res/layout/` o componentes equivalentes en Jetpack Compose si se realiza una migración.
