package com.calidad.tareas.legado;

import static org.assertj.core.api.Assertions.assertThat;

import com.calidad.tareas.dominio.EstadoTarea;
import com.calidad.tareas.dominio.Prioridad;
import com.calidad.tareas.dominio.Tarea;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Red de seguridad antes de refactorizar el código legado: fija su comportamiento actual.
 */
class ReporteLegadoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 15);
    private final ReporteLegado reporte = new ReporteLegado();

    private static Tarea tarea(String titulo, Prioridad prioridad, LocalDate fechaLimite, EstadoTarea... estados) {
        Tarea tarea = new Tarea(titulo, null, prioridad, fechaLimite, Instant.EPOCH);
        for (EstadoTarea estado : estados) {
            tarea.cambiarEstado(estado);
        }
        return tarea;
    }

    private static List<Tarea> tareas() {
        return List.of(
                tarea("Atrasada", Prioridad.ALTA, HOY.minusDays(2)),
                tarea("A tiempo", Prioridad.BAJA, HOY.plusDays(2)),
                tarea("Sin fecha", Prioridad.MEDIA, null),
                tarea("En curso atrasada", Prioridad.CRITICA, HOY.minusDays(1), EstadoTarea.EN_PROGRESO),
                tarea("En curso", Prioridad.BAJA, null, EstadoTarea.EN_PROGRESO),
                tarea("Hecha", Prioridad.MEDIA, null, EstadoTarea.EN_PROGRESO, EstadoTarea.COMPLETADA),
                tarea("Descartada", Prioridad.BAJA, null, EstadoTarea.CANCELADA));
    }

    @Test
    void resumenConCanceladas() {
        String texto = reporte.resumen(tareas(), HOY, true);

        assertThat(texto)
                .contains("VENCIDA: Atrasada", "VENCIDA: En curso atrasada")
                .endsWith("Pendientes: 3, En progreso: 2, Completadas: 1, Canceladas: 1, VENCIDA: 2");
    }

    @Test
    void resumenSinCanceladas() {
        assertThat(reporte.resumen(tareas(), HOY, false))
                .endsWith("Pendientes: 3, En progreso: 2, Completadas: 1, VENCIDA: 2");
    }

    @Test
    void cuentaPorPrioridad() {
        assertThat(reporte.contarPorPrioridad(tareas(), Prioridad.BAJA)).isEqualTo(3);
    }

    @Test
    void formateaConPrioridad() {
        assertThat(reporte.formatear(tarea("Demo", Prioridad.ALTA, null))).isEqualTo("[ALTA] Demo");
    }

    @Test
    void soloAltaYCriticaSonImportantes() {
        assertThat(reporte.esImportante(tarea("a", Prioridad.CRITICA, null))).isTrue();
        assertThat(reporte.esImportante(tarea("b", Prioridad.ALTA, null))).isTrue();
        assertThat(reporte.esImportante(tarea("c", Prioridad.MEDIA, null))).isFalse();
    }
}
