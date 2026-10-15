#!/usr/bin/env bash
# =============================================================================
# Configura SonarQube a partir de quality/quality-gate.json (QUALITY GATE COMO CÓDIGO)
#
#   1. Espera a que el servidor esté UP
#   2. Autenticación:
#        - Si recibe SONAR_TOKEN lo usa (servidor persistente / compartido)
#        - Si no, cambia la clave por defecto admin/admin y genera un token (servidor efímero del pipeline)
#   3. Crea el proyecto, el Quality Profile (reglas de complejidad) y el Quality Gate
#   4. Asocia profile y gate al proyecto
#
# Variables:
#   SONAR_HOST_URL        (def. http://localhost:9000)
#   SONAR_TOKEN           token con permiso de administrar quality gates/profiles (opcional)
#   SONAR_ADMIN_PASSWORD  nueva clave de admin para el servidor efímero (opcional)
#   SONAR_PROJECT_KEY     (def. xiomarag55_analisis-codigo-estatico)
#   QUALITY_CONFIG        (def. quality/quality-gate.json)
#
# Salida: si existe $GITHUB_ENV exporta SONAR_TOKEN para los pasos siguientes.
# Requiere: curl, jq
# =============================================================================
set -euo pipefail

HOST="${SONAR_HOST_URL:-http://localhost:9000}"
PROJECT_KEY="${SONAR_PROJECT_KEY:-xiomarag55_analisis-codigo-estatico}"
CONFIG="${QUALITY_CONFIG:-quality/quality-gate.json}"
ADMIN_PASSWORD="${SONAR_ADMIN_PASSWORD:-Ci-Admin-$(date +%s)-Pwd!}"

log()  { echo -e "\033[1;34m[sonar-setup]\033[0m $*"; }
warn() { echo -e "\033[1;33m[sonar-setup] AVISO:\033[0m $*"; [[ -n "${GITHUB_ACTIONS:-}" ]] && echo "::warning::$*" || true; }

# --- Utilidades HTTP ---------------------------------------------------------
AUTH=()
api_get()  { curl -sS --fail-with-body "${AUTH[@]}" "$HOST/api/$1"; }
api_post() { local path="$1"; shift; curl -sS --fail-with-body "${AUTH[@]}" -X POST "$HOST/api/$path" "$@"; }
# POST que tolera errores (p.ej. "ya existe"): devuelve el cuerpo y no corta el script
api_post_soft() { local path="$1"; shift; curl -sS "${AUTH[@]}" -X POST "$HOST/api/$path" "$@" || true; }

# --- 1. Esperar al servidor --------------------------------------------------
log "Esperando a SonarQube en $HOST ..."
for _ in $(seq 1 90); do
  status=$(curl -s "$HOST/api/system/status" | jq -r '.status // empty' 2>/dev/null || true)
  [[ "$status" == "UP" ]] && break
  sleep 5
done
[[ "${status:-}" == "UP" ]] || { echo "SonarQube no arrancó (estado: ${status:-desconocido})"; exit 1; }
log "SonarQube UP (versión $(curl -s "$HOST/api/server/version"))"

# --- 2. Autenticación --------------------------------------------------------
if [[ -n "${SONAR_TOKEN:-}" ]]; then
  log "Usando SONAR_TOKEN recibido"
  AUTH=(-u "${SONAR_TOKEN}:")
else
  log "Servidor efímero: cambiando la clave por defecto de admin"
  AUTH=(-u "admin:admin")
  if ! api_get authentication/validate | jq -e '.valid' >/dev/null; then
    echo "La clave de admin ya no es 'admin' (servidor ya configurado)."
    echo "Genera un token en $HOST (My Account > Security) y ejecuta: export SONAR_TOKEN=<token>"
    exit 1
  fi
  api_post users/change_password \
    --data-urlencode "login=admin" \
    --data-urlencode "previousPassword=admin" \
    --data-urlencode "password=${ADMIN_PASSWORD}" >/dev/null
  AUTH=(-u "admin:${ADMIN_PASSWORD}")

  SONAR_TOKEN=$(api_post user_tokens/generate --data-urlencode "name=ci-$(date +%s)" | jq -r '.token')
  [[ -n "${GITHUB_ACTIONS:-}" ]] && echo "::add-mask::${SONAR_TOKEN}"
  [[ -n "${GITHUB_ENV:-}" ]] && echo "SONAR_TOKEN=${SONAR_TOKEN}" >> "$GITHUB_ENV"
  [[ -n "${SONAR_TOKEN_FILE:-}" ]] && printf '%s' "$SONAR_TOKEN" > "$SONAR_TOKEN_FILE"
  log "Token de análisis generado"
fi

# --- 3a. Proyecto ------------------------------------------------------------
if ! api_get "projects/search?projects=${PROJECT_KEY}" | jq -e '.components | length > 0' >/dev/null; then
  log "Creando proyecto ${PROJECT_KEY}"
  api_post projects/create --data-urlencode "project=${PROJECT_KEY}" --data-urlencode "name=${PROJECT_KEY}" >/dev/null
fi

# --- 3b. Quality Profile (reglas de complejidad ciclomática y cognitiva) -----
QP_NAME=$(jq -r '.qualityProfile.name' "$CONFIG")

qp_key() {
  api_get "qualityprofiles/search?language=java" \
    | jq -r --arg n "$1" '.profiles[] | select(.name == $n) | .key' | head -n1
}

QP_KEY=$(qp_key "$QP_NAME")
if [[ -z "$QP_KEY" ]]; then
  # Los nombres de los perfiles base cambian entre versiones: se usa el primero que exista
  PARENT_KEY=""
  while read -r parent; do
    PARENT_KEY=$(qp_key "$parent")
    [[ -n "$PARENT_KEY" ]] && break
  done < <(jq -r '.qualityProfile.parents[]' "$CONFIG")
  if [[ -z "$PARENT_KEY" ]]; then
    echo "Ninguno de los perfiles base existe: $(jq -c '.qualityProfile.parents' "$CONFIG")"
    echo "Disponibles: $(api_get 'qualityprofiles/search?language=java' | jq -c '[.profiles[].name]')"
    exit 1
  fi
  log "Creando Quality Profile '${QP_NAME}' a partir de '${parent}'"
  QP_KEY=$(api_post qualityprofiles/copy --data-urlencode "fromKey=${PARENT_KEY}" --data-urlencode "toName=${QP_NAME}" | jq -r '.key')
fi

jq -c '.qualityProfile.rules[]' "$CONFIG" | while read -r rule; do
  key=$(jq -r '.key' <<<"$rule"); params=$(jq -r '.params' <<<"$rule"); desc=$(jq -r '.aprendizaje' <<<"$rule")
  resp=$(api_post_soft qualityprofiles/activate_rule \
    --data-urlencode "key=${QP_KEY}" --data-urlencode "rule=${key}" \
    --data-urlencode "severity=MAJOR" --data-urlencode "params=${params}")
  if jq -e '.errors' >/dev/null 2>&1 <<<"$resp"; then
    warn "No se pudo activar ${key} (${params}): $(jq -r '.errors[0].msg' <<<"$resp")"
    continue
  fi
  # SonarQube ignora en silencio los parámetros con nombre incorrecto: se verifica leyendo la regla
  activos=$(api_get "rules/show?key=${key}&actives=true" \
    | jq -r --arg qp "$QP_KEY" '.actives[] | select(.qProfile == $qp) | .params[] | "\(.key)=\(.value)"')
  IFS=';' read -ra esperados <<<"$params"
  for p in "${esperados[@]}"; do
    grep -qxF "$p" <<<"$activos" || warn "${key}: se pidió '${p}' pero SonarQube tiene '$(tr '\n' ' ' <<<"$activos")'"
  done
  log "Regla ${key} activa con ${params}  -> ${desc}"
done

api_post qualityprofiles/add_project --data-urlencode "language=java" \
  --data-urlencode "qualityProfile=${QP_NAME}" --data-urlencode "project=${PROJECT_KEY}" >/dev/null
log "Quality Profile '${QP_NAME}' asociado a ${PROJECT_KEY}"

# --- 3c. Quality Gate --------------------------------------------------------
QG_NAME=$(jq -r '.qualityGate.name' "$CONFIG")
api_post_soft qualitygates/create --data-urlencode "name=${QG_NAME}" >/dev/null

# Idempotencia: se borran las condiciones actuales y se recrean desde el JSON
api_get "qualitygates/show?name=$(jq -rn --arg v "$QG_NAME" '$v|@uri')" | jq -r '.conditions[]?.id' | while read -r id; do
  api_post qualitygates/delete_condition --data-urlencode "id=${id}" >/dev/null
done

jq -c '.qualityGate.conditions[]' "$CONFIG" | while read -r c; do
  metric=$(jq -r '.metric' <<<"$c"); op=$(jq -r '.op' <<<"$c"); error=$(jq -r '.error' <<<"$c"); desc=$(jq -r '.aprendizaje' <<<"$c")
  resp=$(api_post_soft qualitygates/create_condition --data-urlencode "gateName=${QG_NAME}" \
    --data-urlencode "metric=${metric}" --data-urlencode "op=${op}" --data-urlencode "error=${error}")
  if jq -e '.errors' >/dev/null 2>&1 <<<"$resp"; then
    warn "Condición ${metric} rechazada por SonarQube: $(jq -r '.errors[0].msg' <<<"$resp")"
  else
    log "Condición ${metric} ${op} ${error}  -> ${desc}"
  fi
done

api_post qualitygates/select --data-urlencode "gateName=${QG_NAME}" --data-urlencode "projectKey=${PROJECT_KEY}" >/dev/null
log "Quality Gate '${QG_NAME}' asociado a ${PROJECT_KEY}"
log "Listo. Ejecuta el análisis con: mvn sonar:sonar -Dsonar.host.url=${HOST} -Dsonar.token=<token>"
