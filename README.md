# Tareas API: pipeline CI/CD con quality gates

API REST de gestión de tareas en **Spring Boot 4 / Java 21**, usada como vehículo para aprender y demostrar un pipeline completo de **gestión de la configuración → integración continua → pruebas continuas → quality gates → release**.

```
1 · Configuración ─▶ 2 · Build + pruebas ─▶ 3 · Aceptación Karate ──┐
   versión SemVer      unit · IT ·            sobre la imagen Docker   ├─▶ 5 · Release ─▶ 6 · Despliegue
   enforcer            JaCoCo · PMD                                    │    tag · GHCR       Render + smoke test
   commits                                                             │
1 ───────────────────▶ 4 · Quality Gate SonarQube Cloud ───────────────┘
                          espera el Análisis Automático de ese commit
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
| Fiabilidad, seguridad, mantenibilidad y duplicación del código nuevo | SonarQube Cloud (Análisis Automático) | Quality Gate *Sonar way* | job 4: **bloquea release y despliegue** |
| Versión correcta respondiendo en producción               | Karate `@smoke`          | 100 % de escenarios              | job 6                |

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

## Despliegue en Render

Cada push a `master` que pasa **todos** los gates publica la imagen en GHCR y la despliega en Render. Después, Karate `@smoke` verifica en la URL pública que responde la versión recién liberada.

### Configuración (una sola vez)

1. **Hacer pública la imagen.** Tras el primer release, en GitHub → *Packages* → `tareas-api` → *Package settings* → *Change visibility* → **Public**. Así Render puede descargarla sin credenciales.
2. **Crear el servicio en Render** ([render.com](https://render.com), plan Free):
   *New → Web Service → Existing image* → `ghcr.io/xiomarag55/tareas-api:latest`.
   - *Health Check Path*: `/actuator/health`
   - No hace falta configurar el puerto: la app escucha en la variable `PORT` que asigna Render.
3. En Render → servicio → *Settings* → **Deploy Hook**: copia la URL.
4. En GitHub → *Settings → Secrets and variables → Actions*:
   - **Secret** `RENDER_DEPLOY_HOOK` = la URL del Deploy Hook
   - **Variable** `RENDER_URL` = la URL pública del servicio (p. ej. `https://tareas-api.onrender.com`)
5. *(Opcional)* *Settings → Environments → production → Required reviewers*: exige aprobación manual antes de desplegar.

> En el plan Free, Render apaga el servicio tras 15 min sin tráfico. La primera petición tarda unos 50 s, y el pipeline espera hasta 20 min a que responda la versión nueva.

## Caso de prueba: el Quality Gate bloquea el despliegue

El job 4 espera el análisis que SonarQube Cloud hace de **ese commit** y, si su Quality Gate falla, el release y el despliegue no se ejecutan.

Para comprobarlo, en una rama nueva introduce un bug que SonarQube detecte, por ejemplo comparar textos con `==` (regla `java:S4973`). En `ReporteLegado.java`:

```java
public boolean esUrgente(Tarea t) {
    return t.getTitulo() == "URGENTE";   // bug: compara referencias, no contenido
}
```

```bash
git checkout -b demo/quality-gate-falla
git commit -am "feat(legado): marca tareas urgentes"
git push -u origin demo/quality-gate-falla     # abre un PR hacia master
```

Resultado esperado en el PR:

| Job | Resultado |
|---|---|
| 2 · Build y pruebas | ✅ (el código compila y las pruebas pasan) |
| 4 · Quality Gate SonarQube Cloud | ❌ `new_reliability_rating` = C (umbral A) |

El PR queda en rojo y, si se hiciera merge, el pipeline de `master` se detendría en el job 4 sin liberar ni desplegar. Para que el gate bloquee también el botón de merge, en *Settings → Branches → Branch protection rule* para `master` marca *Require status checks to pass* y selecciona **4 · Quality Gate SonarQube Cloud**.

Corrige con `"URGENTE".equals(t.getTitulo())` y el gate vuelve a pasar.
