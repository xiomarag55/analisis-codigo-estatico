package tareas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import org.junit.jupiter.api.Test;

/**
 * Ejecuta todos los .feature en paralelo y genera el reporte HTML en
 * target/karate-reports/karate-summary.html (el pipeline lo publica como artefacto).
 *
 * Etiquetas: @smoke (rápidas, post-despliegue) | @regresion | @ignore
 *   mvn test -Dkarate.options="--tags @smoke"
 */
class AcceptanceRunner {

    @Test
    void ejecutarFeatures() {
        Results results = Runner.path("classpath:tareas")
                .tags("~@ignore")
                .outputJunitXml(true)
                .outputCucumberJson(true)
                .parallel(4);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}
