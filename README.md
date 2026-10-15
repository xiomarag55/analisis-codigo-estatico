# Tareas API: pipeline CI/CD con quality gates

API REST de gestión de tareas en **Spring Boot 4 / Java 21**, usada como vehículo para aprender y demostrar un pipeline completo de **gestión de la configuración → integración continua → pruebas continuas → quality gates → release**.

```
1 · Configuración ─▶ 2 · Build + pruebas ─▶ 3 · Aceptación Karate ─▶ 4 · Release (master)
   versión SemVer      unit · IT ·            sobre la imagen Docker    tag · GHCR · GitHub Release
   enforcer            JaCoCo · PMD
   commits

SonarQube Cloud ── Análisis Automático en cada push a master (independiente del pipeline)
```

## Estructura

```
├── .github/workflows/ci-cd.yml      Pipeline completo (5 jobs)
├── src/                             API Spring Boot + pruebas unitarias (*Test) e integración (*IT)
├── acceptance-tests/                Pruebas de aceptación Karate (módulo independiente, caja negra)
├── quality/
│   ├── quality-gate.json            Quality Gate y Quality Profile de SonarQube COMO CÓDIGO
│   └── pmd-ruleset.xml              Gate local de complejidad ciclomática y cognitiva
├── scripts/
│   ├── sonar/setup-sonarqube.sh     Aplica quality-gate.json vía Web API
│   ├── sonar/quality-report.sh      Reporte de métricas (complejidad, deuda, cobertura)
│   ├── release/next-version.sh      SemVer automático desde Conventional Commits
│   ├── release/check-commits.sh     Valida Conventional Commits en los PR
│   └── local/pipeline-local.sh      Reproduce el pipeline en tu máquina
├── docs/aprendizaje/                Ruta de aprendizaje (7 módulos) + bitácora
├── labs/                            Código defectuoso a propósito para romper los gates
├── Dockerfile · docker-compose.yml
```

## Quality gates

| Gate                                                      | Herramienta              | Umbral                           | Dónde falla          |
| --------------------------------------------------------- | ------------------------ | -------------------------------- | -------------------- |
| Cobertura de líneas / ramas                               | JaCoCo                   | ≥ 80 % / ≥ 70 %                  | `mvn verify` (job 2) |
| Complejidad **ciclomática** por método                    | PMD + Sonar `java:S1541` | ≤ 10                             | `mvn verify` (job 2) |
| Complejidad **cognitiva** por método                      | PMD + Sonar `java:S3776` | ≤ 15                             | `mvn verify` (job 2) |
| Comportamiento + versión desplegada                       | Karate                   | 100 % de escenarios              | job 3                |
| Duplicación, ratings, deuda técnica                       | SonarQube Cloud (Análisis Automático) | Quality Gate de SonarQube Cloud | informativo, no bloquea el release |

## Uso local

Requisitos: Java 21, Maven 3.9+, Docker. Para los scripts en Windows usa Git Bash o WSL (necesitan `jq`).

```bash
mvn verify                                   # build + unitarias + integración + cobertura + complejidad
mvn spring-boot:run                          # API en http://localhost:8080/api/v1/tareas

# Pruebas de aceptación (con la API levantada)
mvn -f acceptance-tests/pom.xml test -Dkarate.env=local

# Pipeline completo en local (incluye SonarQube en Docker, http://localhost:9000)
bash scripts/local/pipeline-local.sh
```

Reportes: `target/site/jacoco-merged/index.html`, `acceptance-tests/target/karate-reports/karate-summary.html`, `reports/quality-report.md`.

## API

| Método | Ruta                                 | Descripción                                             |
| ------ | ------------------------------------ | ------------------------------------------------------- |
| GET    | `/api/v1/tareas?estado=`             | Listar (filtro opcional)                                |
| GET    | `/api/v1/tareas/{id}`                | Obtener                                                 |
| POST   | `/api/v1/tareas`                     | Crear `{titulo, descripcion?, prioridad, fechaLimite?}` |
| PUT    | `/api/v1/tareas/{id}`                | Actualizar                                              |
| PATCH  | `/api/v1/tareas/{id}/estado`         | Cambiar estado `{estado}`                               |
| DELETE | `/api/v1/tareas/{id}`                | Eliminar                                                |
| GET    | `/actuator/health`, `/actuator/info` | Salud, versión y commit                                 |

Estados: `PENDIENTE → EN_PROGRESO → COMPLETADA`, con `CANCELADA` como estado final alternativo.
