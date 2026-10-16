package com.calidad.tareas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos de la documentación OpenAPI. La versión sale de build-info, así Swagger
 * muestra la misma versión que /actuator/info y que el release (trazabilidad).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tareasOpenApi(ObjectProvider<BuildProperties> buildProperties) {
        String version = buildProperties.stream().map(BuildProperties::getVersion).findFirst().orElse("dev");
        return new OpenAPI().info(new Info()
                .title("Tareas API")
                .description("API de gestión de tareas - proyecto de aprendizaje de CI/CD y calidad de software")
                .version(version)
                .license(new License().name("Uso educativo")));
    }
}
