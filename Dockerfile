# Imagen de ejecución. El jar ya viene construido y probado por el pipeline
# ("build once, deploy many"): la misma imagen que pasa Karate es la que se libera.
FROM eclipse-temurin:21-jre-alpine

ARG VERSION=dev
ARG REVISION=unknown
LABEL org.opencontainers.image.title="tareas-api" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.revision="${REVISION}" \
      org.opencontainers.image.description="API de gestión de tareas - proyecto de calidad CI/CD"

RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --chown=app:app target/tareas-api.jar app.jar
USER app

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
  CMD wget -qO- http://localhost:${SERVER_PORT:-${PORT:-8080}}/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
