# Bolso Inteligente IoT — Android

Aplicación Android en Kotlin y Jetpack Compose para supervisar el estado del
bolso. La interfaz puede funcionar en dos fuentes claramente diferenciadas:

- **Datos de prueba:** se activa cuando no hay configuración de Supabase.
- **Supabase Realtime:** se activa al compilar con URL y clave publicable
  válidas; muestra los cambios enviados por el ESP32.

## Estado actual

- Dashboard de seguridad: armado, apertura, llaves, billetera y alarma.
- Historial de eventos creado por la Edge Function.
- Indicador visible de conexión: datos de prueba, conectando, esperando ESP32,
  en vivo o sin conexión.
- Realtime mediante postgrest-kt y realtime-kt.
- Sin claves administrativas dentro del APK.

Firebase Cloud Messaging no forma parte todavía de esta etapa. Realtime mantiene
la interfaz actualizada mientras la app está abierta; FCM será la capa posterior
para alertas cuando esté cerrada.

## Configurar Supabase

1. Copie local.properties.example como local.properties.
2. Complete los valores publicables de su proyecto:

~~~properties
SUPABASE_URL=https://tu-project-ref.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_...
SUPABASE_BAG_ID=bolso-demo-001
~~~

3. Siga el despliegue documentado en ../INTEGRACION_SUPABASE.md.
4. Sincronice Gradle y ejecute app en un dispositivo con Android 8.0
   (API 26) o superior.

local.properties no se versiona. La URL y la clave publicable pueden existir
en una app Android, pero claves secretas, secretos de Edge Functions y tokens
administrativos nunca deben añadirse.

## Compilar

Desde la carpeta android:

~~~powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
~~~

El APK de prueba queda en:

~~~text
app\build\outputs\apk\debug\app-debug.apk
~~~

## Estructura

~~~text
app/src/main/java/com/securitybag/app/
├── data/fake/          escenarios locales demostrables
├── data/repository/    selección de fuente de datos
├── data/supabase/      cliente y mapeo de Supabase Realtime
├── domain/             modelos y contrato de repositorio
└── ui/                 dashboard, historial, navegación y tema
~~~
