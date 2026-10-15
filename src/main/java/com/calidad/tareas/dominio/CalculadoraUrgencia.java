package com.calidad.tareas.dominio;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Calcula un puntaje de urgencia (0-100) combinando prioridad y cercanía de la fecha límite.
 *
 * <p>Ejemplo didáctico de BAJA complejidad: cada decisión vive en un método pequeño
 * con un único nivel de anidamiento. Compárese con labs/01-complejidad/UrgenciaCompleja.java.txt,
 * que resuelve lo mismo con complejidad cognitiva &gt; 15 y rompe el quality gate.
 */
public final class CalculadoraUrgencia {

    static final int MAXIMO = 100;
    private static final int PUNTOS_POR_PESO = 10;

    private CalculadoraUrgencia() {
    }

    public static int calcular(Tarea tarea, LocalDate hoy) {
        if (tarea.getEstado().esFinal()) {
            return 0;
        }
        int puntaje = tarea.getPrioridad().peso() * PUNTOS_POR_PESO + puntosPorFecha(tarea.getFechaLimite(), hoy);
        return Math.min(puntaje, MAXIMO);
    }

    private static int puntosPorFecha(LocalDate fechaLimite, LocalDate hoy) {
        if (fechaLimite == null) {
            return 0;
        }
        long dias = ChronoUnit.DAYS.between(hoy, fechaLimite);
        if (dias < 0) {
            return MAXIMO;
        }
        if (dias <= 1) {
            return 40;
        }
        return dias <= 7 ? 20 : 0;
    }
}
