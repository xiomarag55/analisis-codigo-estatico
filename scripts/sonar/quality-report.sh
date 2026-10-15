#!/usr/bin/env bash
# =============================================================================
# Reporte de MÉTRICAS DE CALIDAD tras el análisis de SonarQube.
#
# Consulta la Web API y genera:
#   - reports/quality-report.json  (histórico: se guarda como artefacto en cada ejecución)
#   - reports/quality-report.md    (se adjunta al Release y al resumen del pipeline)
#
# Además evalúa los límites de "reportThresholds" de quality/quality-gate.json
# (complejidad media por método y deuda técnica total), que SonarQube no permite
# usar como condiciones de un Quality Gate.
#
# Variables: SONAR_HOST_URL, SONAR_TOKEN, SONAR_PROJECT_KEY, QUALITY_CONFIG
# Requiere: curl, jq, awk
# =============================================================================
set -euo pipefail

HOST="${SONAR_HOST_URL:-http://localhost:9000}"
PROJECT_KEY="${SONAR_PROJECT_KEY:-tareas-api}"
CONFIG="${QUALITY_CONFIG:-quality/quality-gate.json}"
OUT_DIR="${REPORT_DIR:-reports}"
mkdir -p "$OUT_DIR"

api() { curl -sS --fail-with-body -u "${SONAR_TOKEN}:" "$HOST/api/$1"; }

# Espera a que termine el procesamiento del último análisis (Compute Engine)
for _ in $(seq 1 60); do
  pending=$(api "ce/component?component=${PROJECT_KEY}" | jq '(.queue | length) + (if .current.status == "IN_PROGRESS" then 1 else 0 end)')
  [[ "$pending" == "0" ]] && break
  sleep 3
done

METRICS="ncloc,functions,classes,complexity,cognitive_complexity,coverage,line_coverage,branch_coverage,tests,\
sqale_index,sqale_rating,sqale_debt_ratio,code_smells,bugs,vulnerabilities,security_hotspots,\
reliability_rating,security_rating,duplicated_lines_density"

measures=$(api "measures/component?component=${PROJECT_KEY}&metricKeys=${METRICS}" \
  | jq '[.component.measures[] | {key: .metric, value: .value}] | from_entries')
gate=$(api "qualitygates/project_status?projectKey=${PROJECT_KEY}" | jq '.projectStatus')
top=$(api "measures/component_tree?component=${PROJECT_KEY}&qualifiers=FIL&metricKeys=complexity,cognitive_complexity&s=metric&metricSort=cognitive_complexity&asc=false&ps=5" \
  | jq '[.components[] | {archivo: .path, medidas: ([.measures[] | {key: .metric, value: .value}] | from_entries)}]')

m() { jq -r --arg k "$1" '.[$k] // "0"' <<<"$measures"; }
rating() { case "${1%%.*}" in 1) echo A;; 2) echo B;; 3) echo C;; 4) echo D;; 5) echo E;; *) echo "-";; esac; }
div() { awk -v a="$1" -v b="$2" 'BEGIN { printf "%.2f", (b > 0 ? a / b : 0) }'; }
gt()  { awk -v a="$1" -v b="$2" 'BEGIN { exit !(a > b) }'; }

functions=$(m functions)
cc_avg=$(div "$(m complexity)" "$functions")
cog_avg=$(div "$(m cognitive_complexity)" "$functions")
debt=$(m sqale_index)

max_cc=$(jq -r '.reportThresholds.complexity_per_function_max' "$CONFIG")
max_cog=$(jq -r '.reportThresholds.cognitive_complexity_per_function_max' "$CONFIG")
max_debt=$(jq -r '.reportThresholds.sqale_index_minutes_max' "$CONFIG")

fallos=0
check() { if gt "$1" "$2"; then fallos=$((fallos + 1)); echo "❌"; else echo "✅"; fi; }
s_cc=$(check "$cc_avg" "$max_cc"); s_cog=$(check "$cog_avg" "$max_cog"); s_debt=$(check "$debt" "$max_debt")
gate_status=$(jq -r '.status' <<<"$gate")

# --- JSON (evidencia del aprendizaje: compara ejecuciones a lo largo del tiempo) ---
jq -n --arg fecha "$(date -u +%FT%TZ)" --arg commit "${GITHUB_SHA:-local}" --arg version "${APP_VERSION:-}" \
  --argjson medidas "$measures" --argjson gate "$gate" --argjson top "$top" \
  --arg cc "$cc_avg" --arg cog "$cog_avg" \
  '{fecha: $fecha, commit: $commit, version: $version, qualityGate: $gate.status,
    complejidadMediaPorMetodo: {ciclomatica: ($cc|tonumber), cognitiva: ($cog|tonumber)},
    medidas: $medidas, condiciones: $gate.conditions, archivosMasComplejos: $top}' \
  > "$OUT_DIR/quality-report.json"

# --- Markdown -------------------------------------------------------------------
{
  echo "## 📊 Reporte de calidad — Quality Gate: $([[ $gate_status == OK ]] && echo '✅ APROBADO' || echo "❌ $gate_status")"
  echo
  echo "### Condiciones del Quality Gate (SonarQube)"
  echo "| Métrica | Valor | Operador | Umbral | Estado |"
  echo "|---|---|---|---|---|"
  jq -r '.conditions[] | "| `\(.metricKey)` | \(.actualValue // "n/a") | \(.comparator) | \(.errorThreshold) | \(if .status == "OK" then "✅" elif .status == "ERROR" then "❌" else .status end) |"' <<<"$gate"
  echo
  echo "> En un servidor efímero cada análisis es el primero: las condiciones \`new_*\` (código nuevo) no tienen valor. Ver docs/aprendizaje/04-quality-gates.md"
  echo
  echo "### Complejidad (límites en quality/quality-gate.json → reportThresholds)"
  echo "| Métrica | Valor | Límite | Estado |"
  echo "|---|---|---|---|"
  echo "| Complejidad ciclomática total | $(m complexity) | — | ℹ️ |"
  echo "| Complejidad cognitiva total | $(m cognitive_complexity) | — | ℹ️ |"
  echo "| Ciclomática media por método | $cc_avg | $max_cc | $s_cc |"
  echo "| Cognitiva media por método | $cog_avg | $max_cog | $s_cog |"
  echo
  echo "### Deuda técnica y mantenibilidad"
  echo "| Métrica | Valor |"
  echo "|---|---|"
  echo "| Deuda técnica (sqale_index) | ${debt} min (límite ${max_debt}) $s_debt |"
  echo "| Ratio de deuda técnica | $(m sqale_debt_ratio)% |"
  echo "| Rating de mantenibilidad | $(rating "$(m sqale_rating)") |"
  echo "| Code smells | $(m code_smells) |"
  echo
  echo "### Cobertura, fiabilidad y seguridad"
  echo "| Métrica | Valor |"
  echo "|---|---|"
  echo "| Cobertura total / líneas / ramas | $(m coverage)% / $(m line_coverage)% / $(m branch_coverage)% |"
  echo "| Pruebas | $(m tests) |"
  echo "| Bugs (rating fiabilidad) | $(m bugs) ($(rating "$(m reliability_rating)")) |"
  echo "| Vulnerabilidades (rating seguridad) | $(m vulnerabilities) ($(rating "$(m security_rating)")) |"
  echo "| Security hotspots | $(m security_hotspots) |"
  echo "| Duplicación | $(m duplicated_lines_density)% |"
  echo "| Líneas de código / métodos / clases | $(m ncloc) / ${functions} / $(m classes) |"
  echo
  echo "### Top 5 archivos por complejidad cognitiva (candidatos a refactorizar)"
  echo "| Archivo | Cognitiva | Ciclomática |"
  echo "|---|---|---|"
  jq -r '.[] | "| `\(.archivo)` | \(.medidas.cognitive_complexity // "0") | \(.medidas.complexity // "0") |"' <<<"$top"
} > "$OUT_DIR/quality-report.md"

cat "$OUT_DIR/quality-report.md"
[[ -n "${GITHUB_STEP_SUMMARY:-}" ]] && cat "$OUT_DIR/quality-report.md" >> "$GITHUB_STEP_SUMMARY"

if [[ "$fallos" -gt 0 ]]; then
  echo "❌ $fallos métrica(s) superan los límites de reportThresholds"
  exit 1
fi
