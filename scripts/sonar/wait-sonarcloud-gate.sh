#!/usr/bin/env bash
# =============================================================================
# Espera el Análisis Automático de SonarQube Cloud para UN commit concreto y
# falla si su Quality Gate no pasa. Así el análisis sigue siendo automático
# (lo dispara SonarQube Cloud) pero bloquea el release y el despliegue.
#
#   1. Busca el análisis cuya revisión sea COMMIT_SHA (en la rama o el PR)
#   2. Consulta el Quality Gate de ESE análisis (no el "último" del proyecto)
#   3. exit 0 si OK, exit 1 si ERROR o si el análisis no llega a tiempo
#
# Variables:
#   COMMIT_SHA          commit a verificar (obligatoria)
#   SONAR_PROJECT_KEY   (def. xiomarag55_analisis-codigo-estatico)
#   SONAR_BRANCH        rama analizada (def. master)
#   SONAR_PULL_REQUEST  número de PR (si se indica, tiene prioridad sobre la rama)
#   SONAR_TOKEN         solo si el proyecto es privado
#   SONAR_WAIT_SECONDS  tiempo máximo de espera (def. 600)
# Requiere: curl, jq
# =============================================================================
set -euo pipefail

HOST="https://sonarcloud.io"
PROJECT_KEY="${SONAR_PROJECT_KEY:-xiomarag55_analisis-codigo-estatico}"
SHA="${COMMIT_SHA:?Falta COMMIT_SHA}"
WAIT="${SONAR_WAIT_SECONDS:-600}"

if [[ -n "${SONAR_PULL_REQUEST:-}" ]]; then
  SCOPE="pullRequest=${SONAR_PULL_REQUEST}"
else
  SCOPE="branch=${SONAR_BRANCH:-master}"
fi

AUTH=()
[[ -n "${SONAR_TOKEN:-}" ]] && AUTH=(-u "${SONAR_TOKEN}:")
api() { curl -sS --fail-with-body "${AUTH[@]}" "$HOST/api/$1"; }

echo "Esperando el análisis de SonarQube Cloud del commit ${SHA:0:7} (${SCOPE}, máx. ${WAIT}s)..."
analysis=""
for ((t = 0; t < WAIT; t += 15)); do
  analysis=$(api "project_analyses/search?project=${PROJECT_KEY}&${SCOPE}&ps=20" \
    | jq -r --arg sha "$SHA" '[.analyses[] | select(.revision == $sha)][0].key // empty')
  [[ -n "$analysis" ]] && break
  sleep 15
done

if [[ -z "$analysis" ]]; then
  echo "::error::SonarQube Cloud no analizó el commit ${SHA:0:7} en ${WAIT}s. ¿Está activo el Análisis Automático?"
  exit 1
fi

status=$(api "qualitygates/project_status?analysisId=${analysis}")
gate=$(jq -r '.projectStatus.status' <<<"$status")

{
  echo "## 🔍 Quality Gate de SonarQube Cloud: $([[ $gate == OK ]] && echo '✅ APROBADO' || echo "❌ $gate")"
  echo "Commit \`${SHA:0:7}\` · [ver análisis](${HOST}/summary/new_code?id=${PROJECT_KEY}&${SCOPE})"
  echo
  echo "| Condición (código nuevo) | Valor | Umbral | Estado |"
  echo "|---|---|---|---|"
  jq -r '.projectStatus.conditions[] |
    "| `\(.metricKey)` | \(.actualValue // "—") | \(.comparator) \(.errorThreshold) | \(if .status == "OK" then "✅" else "❌" end) |"' <<<"$status"
} | tee -a "${GITHUB_STEP_SUMMARY:-/dev/null}"

if [[ "$gate" != "OK" ]]; then
  echo "::error::El Quality Gate de SonarQube Cloud falló: no se libera ni se despliega."
  exit 1
fi
