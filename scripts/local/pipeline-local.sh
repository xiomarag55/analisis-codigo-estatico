#!/usr/bin/env bash
# =============================================================================
# Reproduce en tu máquina las etapas del pipeline, para aprender qué hace cada una.
# Requiere: Java 21, Maven, Docker, curl, jq (en Windows: Git Bash o WSL).
#
#   bash scripts/local/pipeline-local.sh            # todo
#   bash scripts/local/pipeline-local.sh sin-sonar  # sin análisis de SonarQube
# =============================================================================
set -euo pipefail
cd "$(dirname "$0")/../.."

etapa() { echo; echo -e "\033[1;35m══════ $* ══════\033[0m"; }

etapa "1. Gestión de la configuración"
VERSION=$(bash scripts/release/next-version.sh 2>/dev/null || echo "1.0.0")-local
echo "Versión calculada: $VERSION"
mvn -B -q validate -Drevision="$VERSION"

etapa "2. Integración continua + pruebas (unitarias, integración, cobertura, complejidad)"
mvn -B verify -Drevision="$VERSION"

if [[ "${1:-}" != "sin-sonar" ]]; then
  etapa "3. Análisis de calidad (SonarQube)"
  docker compose up -d sonarqube
  token_file=$(mktemp)
  # Si ya configuraste el servidor antes, exporta SONAR_TOKEN y no se cambia la clave
  SONAR_TOKEN_FILE="$token_file" bash scripts/sonar/setup-sonarqube.sh
  export SONAR_TOKEN="${SONAR_TOKEN:-$(cat "$token_file")}"
  rm -f "$token_file"
  mvn -B sonar:sonar -Drevision="$VERSION" -Dsonar.host.url=http://localhost:9000 \
    -Dsonar.token="$SONAR_TOKEN" -Dsonar.qualitygate.wait=true || echo "⚠ Quality Gate no aprobado"
  APP_VERSION="$VERSION" bash scripts/sonar/quality-report.sh || true
fi

etapa "4. Pruebas de aceptación (Karate) contra el contenedor"
docker build -t "tareas-api:$VERSION" --build-arg VERSION="$VERSION" .
docker rm -f tareas-api-aceptacion >/dev/null 2>&1 || true
docker run -d --name tareas-api-aceptacion -p 8080:8080 -e SPRING_PROFILES_ACTIVE=ci "tareas-api:$VERSION"
trap 'docker rm -f tareas-api-aceptacion >/dev/null 2>&1 || true' EXIT
for _ in $(seq 1 30); do curl -sf http://localhost:8080/actuator/health >/dev/null && break; sleep 2; done
mvn -B -f acceptance-tests/pom.xml test -Dkarate.env=ci -DexpectedVersion="$VERSION"

etapa "✅ Pipeline local completado. Reportes:"
echo "  Cobertura: target/site/jacoco-merged/index.html"
echo "  Karate:    acceptance-tests/target/karate-reports/karate-summary.html"
echo "  Calidad:   reports/quality-report.md"
