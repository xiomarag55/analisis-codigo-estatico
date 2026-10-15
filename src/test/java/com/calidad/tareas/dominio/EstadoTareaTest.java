package com.calidad.tareas.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class EstadoTareaTest {

    @ParameterizedTest(name = "{0} -> {1} permitido={2}")
    @CsvSource({
        "PENDIENTE,   EN_PROGRESO, true",
        "PENDIENTE,   CANCELADA,   true",
        "PENDIENTE,   COMPLETADA,  false",
        "EN_PROGRESO, COMPLETADA,  true",
        "EN_PROGRESO, PENDIENTE,   true",
        "COMPLETADA,  PENDIENTE,   false",
        "CANCELADA,   EN_PROGRESO, false"
    })
    void validaLasTransiciones(EstadoTarea origen, EstadoTarea destino, boolean permitido) {
        assertThat(origen.puedeTransicionarA(destino)).isEqualTo(permitido);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTarea.class, names = {"COMPLETADA", "CANCELADA"})
    void losEstadosFinalesNoTienenSalida(EstadoTarea estado) {
        assertThat(estado.esFinal()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTarea.class, names = {"PENDIENTE", "EN_PROGRESO"})
    void losEstadosActivosNoSonFinales(EstadoTarea estado) {
        assertThat(estado.esFinal()).isFalse();
    }
}
