package com.calidad.tareas.legado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.calidad.tareas.dominio.EstadoTarea;
import com.calidad.tareas.dominio.Prioridad;
import com.calidad.tareas.dominio.Tarea;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Red de seguridad del código legado: fijó el comportamiento antes de refactorizar, así
 * el refactor no cambió resultados sin querer (salvo el bug de la división entera, corregido).
 */
class ExportadorLegadoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 16);
    private final ExportadorLegado exportador = new ExportadorLegado();

    private static Tarea tarea(String titulo, Prioridad prioridad, LocalDate fechaLimite, EstadoTarea... estados) {
        Tarea tarea = new Tarea(titulo, null, prioridad, fechaLimite, Instant.EPOCH);
        for (EstadoTarea estado : estados) {
            tarea.cambiarEstado(estado);
        }
        return tarea;
    }

    private static List<Tarea> tareas() {
        return List.of(
                tarea("Atrasada", Prioridad.ALTA, HOY.minusDays(1)),
                tarea("En curso", Prioridad.BAJA, null, EstadoTarea.EN_PROGRESO),
                tarea("Hecha", Prioridad.MEDIA, null, EstadoTarea.EN_PROGRESO, EstadoTarea.COMPLETADA));
    }

    @Test
    void exportaCsvConTotales() {
        assertThat(exportador.exportarCsv(tareas(), HOY))
                .contains("null;Atrasada;ALTA;PENDIENTE")
                .contains("total;3", "pendientes;1", "enProgreso;1", "completadas;1", "vencidas;1");
    }

    @Test
    void exportaTextoSinSeparadores() {
        assertThat(exportador.exportarTexto(tareas(), HOY))
                .contains("null Atrasada ALTA PENDIENTE")
                .contains("total 3", "vencidas 1")
                .doesNotContain(";");
    }

    @Test
    void porcentajeDeCompletadasConservaLosDecimales() {
        // 1 de 3 = 33.33...%; antes la división entera devolvía 33.0 (bug java:S2184)
        assertThat(exportador.porcentajeCompletadas(tareas())).isCloseTo(33.33, within(0.01));
        assertThat(exportador.porcentajeCompletadas(List.of())).isZero();
    }

    @Test
    void construyeElDestinoFtp() {
        assertThat(exportador.destinoFtp()).startsWith("ftp://exportador:").endsWith("/exportaciones");
    }

    @Test
    void laContrasenaFtpNoVieneDelCodigo() {
        assertThat(new ExportadorLegado("desde-entorno").destinoFtp())
                .isEqualTo("ftp://exportador:desde-entorno@ftp.ejemplo.local/exportaciones");
    }

    @Test
    void generaCodigoDeExportacion() {
        assertThat(exportador.codigoExportacion()).matches("EXP-\\d{4}");
    }

    @Test
    void etiquetaSegunPrioridad() {
        assertThat(exportador.etiqueta(tarea("a", Prioridad.CRITICA, null))).isEqualTo("!!!");
        assertThat(exportador.etiqueta(tarea("b", Prioridad.ALTA, null))).isEqualTo("!!");
        assertThat(exportador.etiqueta(tarea("c", Prioridad.BAJA, null))).isEqualTo("!");
    }

    @Test
    void parseaPrioridadConValorPorDefecto() {
        assertThat(exportador.parsearPrioridad(" alta ")).isEqualTo(Prioridad.ALTA);
        assertThat(exportador.parsearPrioridad("desconocida")).isEqualTo(Prioridad.MEDIA);
        assertThat(exportador.parsearPrioridad(null)).isEqualTo(Prioridad.MEDIA);
    }
}
