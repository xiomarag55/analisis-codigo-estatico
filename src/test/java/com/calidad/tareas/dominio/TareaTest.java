package com.calidad.tareas.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TareaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 15);

    private static Tarea tareaCon(LocalDate fechaLimite) {
        return new Tarea("Escribir pruebas", null, Prioridad.MEDIA, fechaLimite, Instant.EPOCH);
    }

    @Test
    void nuevaTareaEmpiezaPendiente() {
        assertThat(tareaCon(null).getEstado()).isEqualTo(EstadoTarea.PENDIENTE);
    }

    @Test
    void cambiaDeEstadoSiLaTransicionEsValida() {
        Tarea tarea = tareaCon(null);

        tarea.cambiarEstado(EstadoTarea.EN_PROGRESO);

        assertThat(tarea.getEstado()).isEqualTo(EstadoTarea.EN_PROGRESO);
    }

    @Test
    void rechazaTransicionInvalida() {
        Tarea tarea = tareaCon(null);

        assertThatThrownBy(() -> tarea.cambiarEstado(EstadoTarea.COMPLETADA))
                .isInstanceOf(TransicionInvalidaException.class)
                .hasMessageContaining("PENDIENTE a COMPLETADA");
    }

    @Test
    void estaVencidaSiLaFechaLimitePasoYNoEstaTerminada() {
        assertThat(tareaCon(HOY.minusDays(1)).estaVencida(HOY)).isTrue();
    }

    @Test
    void noEstaVencidaSinFechaLimiteNiConFechaFutura() {
        assertThat(tareaCon(null).estaVencida(HOY)).isFalse();
        assertThat(tareaCon(HOY.plusDays(1)).estaVencida(HOY)).isFalse();
    }

    @Test
    void unaTareaCanceladaNuncaEstaVencida() {
        Tarea tarea = tareaCon(HOY.minusDays(10));
        tarea.cambiarEstado(EstadoTarea.CANCELADA);

        assertThat(tarea.estaVencida(HOY)).isFalse();
    }

    @Test
    void actualizaSusDatos() {
        Tarea tarea = tareaCon(null);

        tarea.actualizar("Nuevo", "desc", Prioridad.ALTA, HOY);

        assertThat(tarea.getTitulo()).isEqualTo("Nuevo");
        assertThat(tarea.getDescripcion()).isEqualTo("desc");
        assertThat(tarea.getPrioridad()).isEqualTo(Prioridad.ALTA);
        assertThat(tarea.getFechaLimite()).isEqualTo(HOY);
    }
}
