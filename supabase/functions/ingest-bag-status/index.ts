import { createClient } from "npm:@supabase/supabase-js@2";

const DEFAULT_BAG_ID = "bolso-demo-001";

const corsHeaders = {
  "Access-Control-Allow-Headers": "content-type, x-device-key",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Allow-Origin": "*",
};

type AlarmType = "none" | "missing_object" | "opening_detected";

type BagState = {
  bag_id: string;
  armed: boolean;
  bag_open: boolean;
  keys_present: boolean;
  keys_rfid_verified: boolean;
  wallet_present: boolean;
  wallet_rfid_verified: boolean;
  alarm_type: AlarmType;
  alarm_latched: boolean;
  keys_echo_us: number;
  wallet_echo_us: number;
  device_timestamp_ms: number;
};

type PreviousBagState = Omit<BagState, "bag_id" | "keys_echo_us" | "wallet_echo_us" | "device_timestamp_ms">;

type BagEventInsert = {
  bag_id: string;
  event_type: string;
  title: string;
  description: string;
  severity: "info" | "success" | "warning" | "critical";
};

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") {
    return jsonResponse({ ok: true }, 200);
  }

  if (request.method !== "POST") {
    return jsonResponse({ error: "method_not_allowed" }, 405);
  }

  const expectedDeviceKey = Deno.env.get("BAG_DEVICE_API_KEY");
  if (
    expectedDeviceKey &&
    request.headers.get("x-device-key") !== expectedDeviceKey
  ) {
    return jsonResponse({ error: "unauthorized_device" }, 401);
  }

  let payload: unknown;
  try {
    payload = await request.json();
  } catch {
    return jsonResponse({ error: "invalid_json" }, 400);
  }

  const state = parseBagState(payload);
  if (!state) {
    return jsonResponse({ error: "invalid_payload" }, 400);
  }

  if (state.bag_id !== DEFAULT_BAG_ID) {
    return jsonResponse({ error: "unknown_bag" }, 403);
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const secretKey = readSupabaseSecretKey();
  if (!supabaseUrl || !secretKey) {
    console.error("Supabase Edge Function secrets are unavailable.");
    return jsonResponse({ error: "server_not_configured" }, 500);
  }

  const admin = createClient(supabaseUrl, secretKey, {
    auth: {
      autoRefreshToken: false,
      persistSession: false,
    },
  });

  const { data: previous, error: previousError } = await admin
    .from("bag_status")
    .select(
      "armed, bag_open, keys_present, keys_rfid_verified, wallet_present, wallet_rfid_verified, alarm_type, alarm_latched",
    )
    .eq("bag_id", state.bag_id)
    .maybeSingle();

  if (previousError) {
    console.error(previousError);
    return jsonResponse({ error: "previous_state_not_available" }, 500);
  }

  const { error: statusError } = await admin
    .from("bag_status")
    .upsert(state, { onConflict: "bag_id" });

  if (statusError) {
    console.error(statusError);
    return jsonResponse({ error: "status_not_saved" }, 500);
  }

  const events = buildEvents(previous as PreviousBagState | null, state);
  if (events.length > 0) {
    const { error: eventsError } = await admin.from("bag_events").insert(events);
    if (eventsError) {
      console.error(eventsError);
      return jsonResponse({ error: "status_saved_but_events_not_saved" }, 500);
    }
  }

  return jsonResponse({
    accepted: true,
    bag_id: state.bag_id,
    events_created: events.length,
  });
});

function parseBagState(value: unknown): BagState | null {
  if (!isRecord(value)) {
    return null;
  }

  const bagId = value["bag_id"];
  const alarmType = value["alarm_type"];
  if (
    typeof bagId !== "string" ||
    bagId.length === 0 ||
    bagId.length > 64 ||
    !isAlarmType(alarmType)
  ) {
    return null;
  }

  const booleans = [
    value["armed"],
    value["bag_open"],
    value["keys_present"],
    value["keys_rfid_verified"],
    value["wallet_present"],
    value["wallet_rfid_verified"],
    value["alarm_latched"],
  ];

  const measurements = [
    value["keys_echo_us"],
    value["wallet_echo_us"],
    value["device_timestamp_ms"],
  ];

  if (!booleans.every((item) => typeof item === "boolean")) {
    return null;
  }
  if (!measurements.every(isNonNegativeInteger)) {
    return null;
  }

  return {
    bag_id: bagId,
    armed: value["armed"] as boolean,
    bag_open: value["bag_open"] as boolean,
    keys_present: value["keys_present"] as boolean,
    keys_rfid_verified: value["keys_rfid_verified"] as boolean,
    wallet_present: value["wallet_present"] as boolean,
    wallet_rfid_verified: value["wallet_rfid_verified"] as boolean,
    alarm_type: alarmType,
    alarm_latched: value["alarm_latched"] as boolean,
    keys_echo_us: value["keys_echo_us"] as number,
    wallet_echo_us: value["wallet_echo_us"] as number,
    device_timestamp_ms: value["device_timestamp_ms"] as number,
  };
}

function buildEvents(
  previous: PreviousBagState | null,
  current: BagState,
): BagEventInsert[] {
  if (!previous) {
    return [
      event(
        current.bag_id,
        "device_connected",
        "Dispositivo conectado",
        "Se recibió la primera telemetría del bolso.",
        "info",
      ),
    ];
  }

  const events: BagEventInsert[] = [];

  if (previous.armed !== current.armed) {
    events.push(
      event(
        current.bag_id,
        current.armed ? "system_armed" : "system_disarmed",
        current.armed ? "Sistema armado" : "Sistema desarmado",
        current.armed
          ? "El monitoreo del bolso fue activado."
          : "La alarma fue reiniciada manualmente.",
        current.armed ? "success" : "info",
      ),
    );
  }

  appendItemEvents(events, previous, current, "keys");
  appendItemEvents(events, previous, current, "wallet");

  if (
    previous.alarm_type !== "opening_detected" &&
    current.alarm_type === "opening_detected"
  ) {
    events.push(
      event(
        current.bag_id,
        "opening_detected",
        "Apertura detectada",
        "La alarma queda activa hasta que el sistema se desarme.",
        "critical",
      ),
    );
  } else if (
    previous.bag_open !== current.bag_open &&
    current.bag_open &&
    !current.armed
  ) {
    events.push(
      event(
        current.bag_id,
        "bag_opened",
        "Bolso abierto",
        "El reed switch detectó apertura con el sistema desarmado.",
        "info",
      ),
    );
  }

  return events;
}

function appendItemEvents(
  events: BagEventInsert[],
  previous: PreviousBagState,
  current: BagState,
  item: "keys" | "wallet",
) {
  const label = item === "keys" ? "Llaves" : "Billetera";
  const wasPresent = item === "keys"
    ? previous.keys_present
    : previous.wallet_present;
  const isPresent = item === "keys"
    ? current.keys_present
    : current.wallet_present;
  const wasVerified = item === "keys"
    ? previous.keys_rfid_verified
    : previous.wallet_rfid_verified;
  const isVerified = item === "keys"
    ? current.keys_rfid_verified
    : current.wallet_rfid_verified;

  if (wasPresent !== isPresent) {
    events.push(
      event(
        current.bag_id,
        item + (isPresent ? "_returned" : "_removed"),
        label + (isPresent ? " devueltas" : " retiradas"),
        isPresent
          ? "El sensor ultrasónico volvió a detectar el objeto."
          : "El sensor ultrasónico dejó de detectar el objeto.",
        isPresent ? "success" : "warning",
      ),
    );
  }

  if (!wasVerified && isVerified) {
    events.push(
      event(
        current.bag_id,
        item + "_rfid_verified",
        label + " verificadas",
        "La identidad RFID fue confirmada.",
        "success",
      ),
    );
  }
}

function event(
  bagId: string,
  eventType: string,
  title: string,
  description: string,
  severity: BagEventInsert["severity"],
): BagEventInsert {
  return {
    bag_id: bagId,
    event_type: eventType,
    title,
    description,
    severity,
  };
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function isNonNegativeInteger(value: unknown): value is number {
  return typeof value === "number" && Number.isSafeInteger(value) && value >= 0;
}

function isAlarmType(value: unknown): value is AlarmType {
  return value === "none" ||
    value === "missing_object" ||
    value === "opening_detected";
}

function readSupabaseSecretKey(): string | undefined {
  const serializedSecretKeys = Deno.env.get("SUPABASE_SECRET_KEYS");
  if (serializedSecretKeys) {
    try {
      const secretKeys = JSON.parse(serializedSecretKeys);
      if (typeof secretKeys["default"] === "string") {
        return secretKeys["default"];
      }
    } catch {
      console.error("SUPABASE_SECRET_KEYS is not valid JSON.");
    }
  }

  // Compatibilidad temporal con proyectos que todavía usan service_role.
  return Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
}

function jsonResponse(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      ...corsHeaders,
      "Content-Type": "application/json",
    },
  });
}
