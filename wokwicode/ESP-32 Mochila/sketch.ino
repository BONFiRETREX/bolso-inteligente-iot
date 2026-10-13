
#include <SPI.h>
#include <MFRC522.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <WiFiClientSecure.h>

// =====================================
// BOLSO INTELIGENTE IoT - WOKWI
// ESP32 + 2 RFID + 2 HC-SR04
// REED + BOTON + LED + BUZZER
// =====================================

// PINES RFID
#define SS_LLAVES 5
#define SS_BILLETERA 13
#define RST_RFID 21

// SENSORES ULTRASONICOS
#define TRIG_LLAVES 4
#define ECHO_LLAVES 34

#define TRIG_BILLETERA 33
#define ECHO_BILLETERA 14

// CONTROL
#define REED 22
#define BOTON 32

// SALIDAS
#define LED_VERDE 26
#define LED_ROJO 25
#define BUZZER 27

// LECTORES RFID
MFRC522 llaves(SS_LLAVES, RST_RFID);
MFRC522 billetera(SS_BILLETERA, RST_RFID);

// UID AUTORIZADOS
const byte uidLlaves[4] = {
  0x01, 0x02, 0x03, 0x04
};

const byte uidBilletera[4] = {
  0x11, 0x22, 0x33, 0x44
};

// =====================================
// VARIABLES
// =====================================

// =====================================
// NUBE / SUPABASE
// =====================================
// La lógica electrónica funciona aunque esta capa esté desactivada.
// La URL pública de la Edge Function ya está configurada. Tras desplegarla,
// cambie CLOUD_SYNC_ENABLED a 1. No coloque service_role ni secretos en Wokwi público.
#define CLOUD_SYNC_ENABLED 0

// Solo para el simulador Wokwi. En un ESP32 físico se debe validar el
// certificado TLS de Supabase y poner esta opción en 0.
#define CLOUD_ALLOW_INSECURE_TLS 1

const char* CLOUD_WIFI_SSID = "Wokwi-GUEST";
const char* CLOUD_FUNCTION_URL =
  "https://gcfusjuzczyftyslnitw.supabase.co/functions/v1/ingest-bag-status";
const char* CLOUD_BAG_ID = "bolso-demo-001";

// Se admite únicamente para hardware o proyecto no público. Déjelo vacío
// durante la demostración pública de Wokwi.
const char* CLOUD_DEVICE_KEY = "";

const unsigned long CLOUD_HEARTBEAT_MS = 10000UL;
const unsigned long CLOUD_RETRY_MS = 3000UL;
const unsigned long CLOUD_WIFI_RETRY_MS = 8000UL;

bool armado = false;
bool alarmaApertura = false;

bool llavesRegistradas = false;
bool billeteraRegistrada = false;

bool llavesPresentes = false;
bool billeteraPresente = false;

bool botonAnterior = HIGH;

unsigned long ecoLlaves = 0;
unsigned long ecoBilletera = 0;

unsigned long ultimaMedicion = 0;
unsigned long ultimoReporte = 0;
unsigned long ultimoEnvioNube = 0;
unsigned long ultimoIntentoEnvioNube = 0;
unsigned long ultimoIntentoWifi = 0;

bool nubeTieneEstadoEnviado = false;
bool ultimoArmadoNube = false;
bool ultimoBolsoCerradoNube = false;
bool ultimasLlavesPresentesNube = false;
bool ultimaBilleteraPresenteNube = false;
bool ultimasLlavesRegistradasNube = false;
bool ultimaBilleteraRegistradaNube = false;
byte ultimoCodigoAlarmaNube = 255;

// DISTANCIA MAXIMA PERMITIDA
// Se aceptan todas las distancias
// positivas hasta 12 cm.
const unsigned long distanciaMaxima = 12;

// Modelo matematico:
// distancia_cm = tiempo_us * 343 / 20000
// Sin redondeos intermedios.

// =====================================
// MEDIR DISTANCIA
// =====================================

unsigned long medirEco(int trig, int echo) {

  digitalWrite(trig, LOW);
  delayMicroseconds(2);

  digitalWrite(trig, HIGH);
  delayMicroseconds(10);
  digitalWrite(trig, LOW);

  return pulseIn(echo, HIGH, 25000UL);
}

// =====================================
// VALIDAR DISTANCIA SIN REDONDEAR
// =====================================

bool distanciaValida(unsigned long tiempo) {

  // Si no hay eco, el objeto esta ausente.
  if (tiempo == 0) {
    return false;
  }

  // Distancia <= 12 cm.
  // No existe limite minimo de 2 cm.
  // Comparamos enteros sin aproximaciones.

  unsigned long numerador = tiempo * 343UL;

  return numerador <= distanciaMaxima * 20000UL;
}

// =====================================
// IMPRIMIR DISTANCIA EXACTA
// =====================================

void imprimirDistancia(unsigned long tiempo) {

  if (tiempo == 0) {
    Serial.print("SIN ECO");
    return;
  }

  unsigned long numerador = tiempo * 343UL;

  unsigned long parteEntera =
    numerador / 20000UL;

  unsigned long parteDecimal =
    (numerador % 20000UL) * 5UL;

  Serial.print(parteEntera);
  Serial.print(".");

  if (parteDecimal < 10000UL) Serial.print("0");
  if (parteDecimal < 1000UL) Serial.print("0");
  if (parteDecimal < 100UL) Serial.print("0");
  if (parteDecimal < 10UL) Serial.print("0");

  Serial.print(parteDecimal);
  Serial.print(" cm");
}

// =====================================
// ACTUALIZAR PRESENCIA DE OBJETOS
// =====================================




void actualizarPresencia() {

  if (millis() - ultimaMedicion < 300) {
    return;
  }

  ultimaMedicion = millis();

  // Guardar estados anteriores
  bool estadoAnteriorLlaves = llavesPresentes;
  bool estadoAnteriorBilletera = billeteraPresente;

  // Medir distancias
  ecoLlaves = medirEco(TRIG_LLAVES, ECHO_LLAVES);

  ecoBilletera = medirEco(
    TRIG_BILLETERA,
    ECHO_BILLETERA
  );

  // Comprobar presencia hasta 12 cm
  llavesPresentes = distanciaValida(ecoLlaves);

  billeteraPresente = distanciaValida(ecoBilletera);

  // Detectar cambios de presencia
  if (llavesPresentes != estadoAnteriorLlaves) {

    if (llavesPresentes) {
      Serial.println("LLAVES: OBJETO DEVUELTO");
    } else {
      Serial.println("LLAVES: OBJETO RETIRADO");
    }
  }

  if (billeteraPresente != estadoAnteriorBilletera) {

    if (billeteraPresente) {
      Serial.println("BILLETERA: OBJETO DEVUELTO");
    } else {
      Serial.println("BILLETERA: OBJETO RETIRADO");
    }
  }

  // No borrar llavesRegistradas
  // No borrar billeteraRegistrada
}



// =====================================
// VERIFICAR UID
// =====================================

bool comprobarUID(
  MFRC522 &lector,
  const byte esperado[4]
) {

  if (lector.uid.size != 4) {
    return false;
  }

  for (byte i = 0; i < 4; i++) {

    if (lector.uid.uidByte[i] != esperado[i]) {
      return false;
    }
  }

  return true;
}

// =====================================
// LEER TARJETAS RFID
// =====================================

void leerRFID(
  MFRC522 &lector,
  const byte esperado[4],
  bool &registrado,
  bool presente,
  const char* nombre
) {

  if (!lector.PICC_IsNewCardPresent()) {
    return;
  }

  if (!lector.PICC_ReadCardSerial()) {
    return;
  }

  Serial.print("RFID ");
  Serial.print(nombre);
  Serial.print(" UID: ");

  for (byte i = 0; i < lector.uid.size; i++) {

    if (lector.uid.uidByte[i] < 16) {
      Serial.print("0");
    }

    Serial.print(lector.uid.uidByte[i], HEX);
    Serial.print(" ");
  }

  Serial.println();

  if (comprobarUID(lector, esperado)) {

    if (presente) {

      registrado = true;

      Serial.print("OBJETO VERIFICADO: ");
      Serial.println(nombre);

    } else {

      Serial.print("OBJETO IDENTIFICADO PERO AUSENTE: ");
      Serial.println(nombre);
    }

  } else {

    Serial.print("UID NO AUTORIZADO: ");
    Serial.println(nombre);
  }

  lector.PICC_HaltA();
  lector.PCD_StopCrypto1();
}

// =====================================
// ARMAR Y DESARMAR
// =====================================

void controlarBoton() {

  bool botonActual = digitalRead(BOTON);

  if (botonAnterior == HIGH &&
      botonActual == LOW) {

    delay(40);

    if (digitalRead(BOTON) == LOW) {

      armado = !armado;

      if (armado) {

        Serial.println("SISTEMA ARMADO");

      } else {

        alarmaApertura = false;

        digitalWrite(LED_VERDE, LOW);
        digitalWrite(LED_ROJO, LOW);

        noTone(BUZZER);

        Serial.println("SISTEMA DESARMADO");
        Serial.println("ALARMA REINICIADA");
      }
    }
  }

  botonAnterior = digitalRead(BOTON);
}

// =====================================
// CONTROL DE ALARMAS
// =====================================

void controlarAlarmas(
  bool bolsoCerrado,
  bool objetosOK
) {

  // Memorizar una apertura con el
  // sistema armado.
  if (armado && !bolsoCerrado) {
    alarmaApertura = true;
  }

  if (!armado) {

    // SISTEMA DESARMADO
    digitalWrite(LED_VERDE, LOW);
    digitalWrite(LED_ROJO, LOW);

    noTone(BUZZER);
  }

  else if (alarmaApertura) {

    // ALARMA POR APERTURA
    digitalWrite(LED_VERDE, LOW);
    digitalWrite(LED_ROJO, HIGH);

    tone(BUZZER, 1200);
  }

  else if (!objetosOK) {

    // OBJETOS FALTANTES
    bool parpadeo =
      (millis() / 400UL) % 2 == 0;

    digitalWrite(LED_VERDE, LOW);
    digitalWrite(LED_ROJO, parpadeo);

    if (parpadeo) {
      tone(BUZZER, 1000);
    } else {
      noTone(BUZZER);
    }
  }

  else {

    // TODO CORRECTO
    digitalWrite(LED_VERDE, HIGH);
    digitalWrite(LED_ROJO, LOW);

    noTone(BUZZER);
  }
}

// =====================================
// SINCRONIZAR ESTADO CON SUPABASE
// =====================================

byte codigoAlarmaNube(bool objetosOK) {

  if (!armado) {
    return 0;
  }

  if (alarmaApertura) {
    return 2;
  }

  if (!objetosOK) {
    return 1;
  }

  return 0;
}

const char* textoAlarmaNube(byte codigo) {

  if (codigo == 2) {
    return "opening_detected";
  }

  if (codigo == 1) {
    return "missing_object";
  }

  return "none";
}

const char* valorBooleanoJson(bool valor) {
  return valor ? "true" : "false";
}

void iniciarNube() {

#if CLOUD_SYNC_ENABLED

  if (strlen(CLOUD_FUNCTION_URL) == 0) {
    Serial.println("NUBE: URL de Edge Function no configurada");
    return;
  }

  WiFi.mode(WIFI_STA);
  WiFi.begin(CLOUD_WIFI_SSID, "", 6);
  ultimoIntentoWifi = millis();

  Serial.print("NUBE: conectando a ");
  Serial.println(CLOUD_WIFI_SSID);

#else

  Serial.println("NUBE: desactivada (modo simulacion local)");

#endif
}

bool asegurarWifiNube() {

#if CLOUD_SYNC_ENABLED

  if (WiFi.status() == WL_CONNECTED) {
    return true;
  }

  if (millis() - ultimoIntentoWifi < CLOUD_WIFI_RETRY_MS) {
    return false;
  }

  ultimoIntentoWifi = millis();
  WiFi.disconnect();
  WiFi.begin(CLOUD_WIFI_SSID, "", 6);

  Serial.println("NUBE: reintentando Wi-Fi");

#endif

  return false;
}

bool estadoNubeCambio(
  bool bolsoCerrado,
  bool objetosOK,
  byte codigoAlarma
) {

  if (!nubeTieneEstadoEnviado) {
    return true;
  }

  if (armado != ultimoArmadoNube) return true;
  if (bolsoCerrado != ultimoBolsoCerradoNube) return true;
  if (llavesPresentes != ultimasLlavesPresentesNube) return true;
  if (billeteraPresente != ultimaBilleteraPresenteNube) return true;
  if (llavesRegistradas != ultimasLlavesRegistradasNube) return true;
  if (billeteraRegistrada != ultimaBilleteraRegistradaNube) return true;
  if (codigoAlarma != ultimoCodigoAlarmaNube) return true;

  return millis() - ultimoEnvioNube >= CLOUD_HEARTBEAT_MS;
}

void guardarUltimoEstadoNube(
  bool bolsoCerrado,
  byte codigoAlarma
) {

  nubeTieneEstadoEnviado = true;
  ultimoEnvioNube = millis();
  ultimoArmadoNube = armado;
  ultimoBolsoCerradoNube = bolsoCerrado;
  ultimasLlavesPresentesNube = llavesPresentes;
  ultimaBilleteraPresenteNube = billeteraPresente;
  ultimasLlavesRegistradasNube = llavesRegistradas;
  ultimaBilleteraRegistradaNube = billeteraRegistrada;
  ultimoCodigoAlarmaNube = codigoAlarma;
}

String construirPayloadNube(
  bool bolsoCerrado,
  byte codigoAlarma
) {

  String payload = "{";

  payload += "\"bag_id\":\"";
  payload += CLOUD_BAG_ID;
  payload += "\",\"armed\":";
  payload += valorBooleanoJson(armado);
  payload += ",\"bag_open\":";
  payload += valorBooleanoJson(!bolsoCerrado);
  payload += ",\"keys_present\":";
  payload += valorBooleanoJson(llavesPresentes);
  payload += ",\"keys_rfid_verified\":";
  payload += valorBooleanoJson(llavesRegistradas);
  payload += ",\"wallet_present\":";
  payload += valorBooleanoJson(billeteraPresente);
  payload += ",\"wallet_rfid_verified\":";
  payload += valorBooleanoJson(billeteraRegistrada);
  payload += ",\"alarm_type\":\"";
  payload += textoAlarmaNube(codigoAlarma);
  payload += "\",\"alarm_latched\":";
  payload += valorBooleanoJson(alarmaApertura);
  payload += ",\"keys_echo_us\":";
  payload += String(ecoLlaves);
  payload += ",\"wallet_echo_us\":";
  payload += String(ecoBilletera);
  payload += ",\"device_timestamp_ms\":";
  payload += String(millis());
  payload += "}";

  return payload;
}

void publicarEstadoNube(
  bool bolsoCerrado,
  bool objetosOK
) {

#if CLOUD_SYNC_ENABLED

  byte codigoAlarma = codigoAlarmaNube(objetosOK);

  if (!estadoNubeCambio(bolsoCerrado, objetosOK, codigoAlarma)) {
    return;
  }

  if (millis() - ultimoIntentoEnvioNube < CLOUD_RETRY_MS) {
    return;
  }

  ultimoIntentoEnvioNube = millis();

  if (!asegurarWifiNube()) {
    return;
  }

  WiFiClientSecure clienteSeguro;

#if CLOUD_ALLOW_INSECURE_TLS

  clienteSeguro.setInsecure();

#endif

  HTTPClient http;

  if (!http.begin(clienteSeguro, CLOUD_FUNCTION_URL)) {
    Serial.println("NUBE: no se pudo iniciar HTTPS");
    return;
  }

  http.addHeader("Content-Type", "application/json");

  if (strlen(CLOUD_DEVICE_KEY) > 0) {
    http.addHeader("x-device-key", CLOUD_DEVICE_KEY);
  }

  int codigoHttp = http.POST(
    construirPayloadNube(bolsoCerrado, codigoAlarma)
  );

  if (codigoHttp >= 200 && codigoHttp < 300) {
    guardarUltimoEstadoNube(bolsoCerrado, codigoAlarma);

    Serial.print("NUBE: estado enviado, HTTP ");
    Serial.println(codigoHttp);
  } else {
    Serial.print("NUBE: fallo HTTP ");
    Serial.println(codigoHttp);
  }

  http.end();

#endif
}

// =====================================
// CONFIGURACION INICIAL
// =====================================

void setup() {

  Serial.begin(115200);
  delay(500);

  // SENSORES ULTRASONICOS
  pinMode(TRIG_LLAVES, OUTPUT);
  pinMode(ECHO_LLAVES, INPUT);

  pinMode(TRIG_BILLETERA, OUTPUT);
  pinMode(ECHO_BILLETERA, INPUT);

  digitalWrite(TRIG_LLAVES, LOW);
  digitalWrite(TRIG_BILLETERA, LOW);

  // ENTRADAS
  pinMode(REED, INPUT_PULLUP);
  pinMode(BOTON, INPUT_PULLUP);

  // SALIDAS
  pinMode(LED_VERDE, OUTPUT);
  pinMode(LED_ROJO, OUTPUT);
  pinMode(BUZZER, OUTPUT);

  digitalWrite(LED_VERDE, LOW);
  digitalWrite(LED_ROJO, LOW);

  noTone(BUZZER);

  // INICIALIZAR BUS SPI
  pinMode(SS_LLAVES, OUTPUT);
  pinMode(SS_BILLETERA, OUTPUT);

  digitalWrite(SS_LLAVES, HIGH);
  digitalWrite(SS_BILLETERA, HIGH);

  SPI.begin(18, 19, 23);

  // INICIALIZAR RFID
  llaves.PCD_Init();
  delay(100);

  billetera.PCD_Init();
  delay(100);

  // DIAGNOSTICO RFID
  Serial.print("Version RFID LLAVES: 0x");
  Serial.println(
    llaves.PCD_ReadRegister(MFRC522::VersionReg),
    HEX
  );

  Serial.print("Version RFID BILLETERA: 0x");
  Serial.println(
    billetera.PCD_ReadRegister(MFRC522::VersionReg),
    HEX
  );

  // MEDICION INICIAL
  actualizarPresencia();

  Serial.println();
  Serial.println("============================");
  Serial.println("BOLSO INTELIGENTE IoT");
  Serial.println("SIMULACION ELECTRONICA");
  Serial.println("============================");

  Serial.println("Blue Card = LLAVES");
  Serial.println("Green Card = BILLETERA");

  Serial.println("DISTANCIA VALIDA:");
  Serial.println("Mayor que 0 cm y hasta 12 cm");

  Serial.println("REED: APERTURA DEL BOLSO");
  Serial.println("BOTON: ARMAR / DESARMAR");

  iniciarNube();
}

// =====================================
// PROGRAMA PRINCIPAL
// =====================================

void loop() {

  // 1. COMPROBAR PRESENCIA
  actualizarPresencia();

  // 2. VERIFICAR RFID DE LLAVES
  leerRFID(
    llaves,
    uidLlaves,
    llavesRegistradas,
    llavesPresentes,
    "LLAVES"
  );

  // 3. VERIFICAR RFID DE BILLETERA
  leerRFID(
    billetera,
    uidBilletera,
    billeteraRegistrada,
    billeteraPresente,
    "BILLETERA"
  );

  // 4. ARMAR / DESARMAR
  controlarBoton();

  // 5. ESTADO DE APERTURA
  bool bolsoCerrado =
    digitalRead(REED) == LOW;

  // 6. ESTADO DE OBJETOS
  bool llavesOK =
    llavesRegistradas && llavesPresentes;

  bool billeteraOK =
    billeteraRegistrada && billeteraPresente;

  bool objetosOK =
    llavesOK && billeteraOK;

  // 7. CONTROLAR ALARMAS
  controlarAlarmas(bolsoCerrado, objetosOK);

  // 8. ENVIAR CAMBIOS Y HEARTBEAT A SUPABASE
  publicarEstadoNube(bolsoCerrado, objetosOK);

  // 9. MONITOR SERIE
  if (millis() - ultimoReporte >= 1000) {

    ultimoReporte = millis();

    Serial.println("----------------------------");

    Serial.print("Sistema: ");
    Serial.println(
      armado ? "ARMADO" : "DESARMADO"
    );

    Serial.print("Bolso: ");
    Serial.println(
      bolsoCerrado ? "CERRADO" : "ABIERTO"
    );

    Serial.print("Llaves: ");
    Serial.println(
      llavesOK ? "OK" : "FALTAN"
    );

    Serial.print("Billetera: ");
    Serial.println(
      billeteraOK ? "OK" : "FALTA"
    );

    Serial.print("Eco llaves: ");
    Serial.print(ecoLlaves);
    Serial.println(" us");

    Serial.print("Distancia llaves: ");
    imprimirDistancia(ecoLlaves);
    Serial.println();

    Serial.print("Eco billetera: ");
    Serial.print(ecoBilletera);
    Serial.println(" us");

    Serial.print("Distancia billetera: ");
    imprimirDistancia(ecoBilletera);
    Serial.println();

    Serial.print("Alarma: ");

    if (!armado) {
      Serial.println("NO");
    }

    else if (alarmaApertura) {
      Serial.println("APERTURA");
    }

    else if (!objetosOK) {
      Serial.println("OBJETOS FALTANTES");
    }

    else {
      Serial.println("NO");
    }
  }

  delay(30);
}
