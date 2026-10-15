package com.calidad.tareas.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Cada fila cubre una rama de {@link CalculadoraUrgencia}: así la cobertura de ramas
 * (branch coverage) llega al 100% con pruebas legibles.
 */
class CalculadoraUrgenciaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 15);

    @ParameterizedTest(name = "{0} con {1} días restantes -> {2}")
    @CsvSource({
        "BAJA,     30, 10",
        "MEDIA,     7, 40",
        "ALTA,      1, 70",
        "ALTA,      0, 70",
        "BAJA,     -1, 100",
        "CRITICA,   3, 70"
    })
    void combinaPrioridadYFechaLimite(Prioridad prioridad, long diasRestantes, int esperado) {
        Tarea tarea = new Tarea("t", null, prioridad, HOY.plusDays(diasRestantes), Instant.EPOCH);

        assertThat(CalculadoraUrgencia.calcular(tarea, HOY)).isEqualTo(esperado);
    }

    @Test
    void sinFechaLimiteSoloCuentaLaPrioridad() {
        Tarea tarea = new Tarea("t", null, Prioridad.CRITICA, null, Instant.EPOCH);

        assertThat(CalculadoraUrgencia.calcular(tarea, HOY)).isEqualTo(50);
    }

    @Test
    void nuncaSuperaElMaximo() {
        Tarea tarea = new Tarea("t", null, Prioridad.CRITICA, HOY.minusDays(5), Instant.EPOCH);

        assertThat(CalculadoraUrgencia.calcular(tarea, HOY)).isEqualTo(CalculadoraUrgencia.MAXIMO);
    }

    @Test
    void unaTareaFinalizadaNoTieneUrgencia() {
        Tarea tarea = new Tarea("t", null, Prioridad.CRITICA, HOY.minusDays(5), Instant.EPOCH);
        tarea.cambiarEstado(EstadoTarea.CANCELADA);

        assertThat(CalculadoraUrgencia.calcular(tarea, HOY)).isZero();
    }
}
