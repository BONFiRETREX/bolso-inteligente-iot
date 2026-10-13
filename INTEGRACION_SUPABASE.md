# Integración demostrable: Wokwi → Supabase → Android

La ruta de datos de este proyecto es:

~~~text
ESP32 en Wokwi
  → HTTPS POST
Supabase Edge Function
  → PostgreSQL + Realtime
Aplicación Android
~~~

La función de borde escribe usando una clave secreta que existe solamente
en Supabase. La aplicación Android usa una clave **publicable** y solo puede
leer las filas permitidas por RLS.

## Advertencia sobre el proyecto Wokwi público

El proyecto indicado por el estudiante es público. El gateway público de Wokwi
puede observar tráfico y no debe contener claves secretas, contraseñas ni un
secreto permanente de dispositivo. La integración incluida permite una demo
académica con telemetría no sensible, pero un tercero que conozca la URL podría
intentar falsificar estados.

Para una versión real, use un ESP32 físico con secreto provisionado, o Wokwi
privado/VS Code con Private Gateway y configure BAG_DEVICE_API_KEY como
secreto de la Edge Function. No active ese secreto para el enlace público.

## 1. Crear y desplegar Supabase

1. Cree un proyecto de Supabase desde su propia cuenta.
2. Instale la CLI de Supabase si aún no la tiene e inicie sesión.
3. Desde la raíz de este repositorio, ejecute:

~~~powershell
supabase login
supabase link --project-ref TU_PROJECT_REF
supabase db push
supabase functions deploy ingest-bag-status --no-verify-jwt
~~~

La migración crea bag_status y bag_events, habilita Realtime para ambas tablas
y establece políticas RLS de solo lectura para bolso-demo-001. La Edge Function
es el único escritor previsto; el cliente Android no obtiene políticas de
inserción o actualización.

No hace falta copiar una clave secreta: Supabase la suministra a la función
desplegada como variable de entorno. Para un dispositivo físico privado,
opcionalmente configure:

~~~powershell
supabase secrets set BAG_DEVICE_API_KEY=un_valor_largo_aleatorio
~~~

Ese valor no debe usarse en el Wokwi público.

## 2. Configurar el sketch de Wokwi

En wokwicode/ESP-32 Mochila/sketch.ino, cambie únicamente estas opciones:

~~~cpp
#define CLOUD_SYNC_ENABLED 1
const char* CLOUD_FUNCTION_URL =
  "https://TU_PROJECT_REF.supabase.co/functions/v1/ingest-bag-status";
~~~

Mantenga CLOUD_DEVICE_KEY vacío para la demo pública. La simulación conserva
su comportamiento electrónico: RFID, ultrasónicos, reed, botón, LEDs y buzzer
continúan siendo la fuente de verdad. Solo se agregan envíos cuando hay un
cambio de estado o cada 10 segundos como heartbeat.

CLOUD_ALLOW_INSECURE_TLS está en 1 exclusivamente para facilitar la prueba TLS
en Wokwi. Para hardware real debe validarse el certificado de Supabase y
ponerse en 0.

## 3. Configurar y compilar Android

Copie android/local.properties.example a android/local.properties y añada:

~~~properties
SUPABASE_URL=https://TU_PROJECT_REF.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_...
SUPABASE_BAG_ID=bolso-demo-001
~~~

La clave se obtiene en Supabase, en Connect / App Frameworks. Es la clave
publicable (o anon en proyectos antiguos), nunca la de servicio.

Después compile:

~~~powershell
cd android
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
~~~

El APK instalable de depuración se genera en
android\app\build\outputs\apk\debug\app-debug.apk.

## 4. Prueba de extremo a extremo

No se debe declarar integrada hasta observar esta secuencia:

1. Instale la app compilada con la configuración anterior. Debe mostrar
   **CONECTANDO** o **ESPERANDO ESP32** antes del primer mensaje.
2. Inicie la simulación Wokwi. El monitor serie debe informar
   NUBE: estado enviado, HTTP 200.
3. Verifique que aparece una fila en bag_status y el evento inicial en
   bag_events dentro del panel de Supabase.
4. La app debe cambiar a **EN VIVO**.
5. Retire/reponga un objeto, abra el reed o arme/desarme. Confirme el HTTP 2xx,
   la actualización de tablas, el aviso visual y el nuevo elemento del historial.

Realtime actualiza la app activa. Cuando se implemente FCM, la Edge Function o
un trigger de base de datos generará la notificación para una aplicación cerrada.
