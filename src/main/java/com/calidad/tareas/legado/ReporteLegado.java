package com.calidad.tareas.legado;

import com.calidad.tareas.dominio.EstadoTarea;
import com.calidad.tareas.dominio.Prioridad;
import com.calidad.tareas.dominio.Tarea;
import java.time.LocalDate;
import java.util.List;

/**
 * CÓDIGO LEGADO INTENCIONAL, para practicar el análisis en SonarQube.
 *
 * <p>Simula un módulo heredado "que funciona pero nadie quiere tocar". Tiene pruebas
 * (ReporteLegadoTest), así que su comportamiento está protegido y se puede refactorizar con seguridad.
 * PMD lo excluye (ver pom.xml) para que no rompa el build; SonarQube sí lo analiza.
 *
 * <p>Issues que SonarQube debería reportar (todos de mantenibilidad):
 * <ul>
 *   <li>java:S3776 complejidad cognitiva de {@code resumen} mayor que 15</li>
 *   <li>java:S1066 if anidados que se pueden unir</li>
 *   <li>java:S1192 literal de texto repetido</li>
 *   <li>java:S1659 varias variables en una sola declaración</li>
 *   <li>java:S1172 parámetro sin usar</li>
 *   <li>java:S1135 comentario de tarea pendiente</li>
 *   <li>java:S125 código comentado</li>
 *   <li>java:S1488 variable local que se devuelve inmediatamente</li>
 *   <li>java:S1126 if/else que devuelve true/false</li>
 *   <li>java:S1068 campo privado sin usar</li>
 * </ul>
 */
public class ReporteLegado {

    private int version = 1;

    public String resumen(List<Tarea> tareas, LocalDate hoy, boolean incluirCanceladas) {
        StringBuilder texto = new StringBuilder();
        int pendientes = 0, enProgreso = 0, completadas = 0, canceladas = 0, vencidas = 0;
        for (Tarea t : tareas) {
            if (t.getEstado() == EstadoTarea.PENDIENTE) {
                pendientes++;
                if (t.getFechaLimite() != null) {
                    if (t.getFechaLimite().isBefore(hoy)) {
                        vencidas++;
                        texto.append("VENCIDA: ").append(t.getTitulo()).append('\n');
                    }
                }
            } else if (t.getEstado() == EstadoTarea.EN_PROGRESO) {
                enProgreso++;
                if (t.getFechaLimite() != null && t.getFechaLimite().isBefore(hoy)) {
                    vencidas++;
                    texto.append("VENCIDA: ").append(t.getTitulo()).append('\n');
                }
            } else if (t.getEstado() == EstadoTarea.COMPLETADA) {
                completadas++;
            } else {
                if (incluirCanceladas) {
                    canceladas++;
                }
            }
        }
        texto.append("Pendientes: ").append(pendientes)
                .append(", En progreso: ").append(enProgreso)
                .append(", Completadas: ").append(completadas);
        if (incluirCanceladas) {
            texto.append(", Canceladas: ").append(canceladas);
        }
        texto.append(", ").append("VENCIDA: ").append(vencidas);
        return texto.toString();
    }

    public int contarPorPrioridad(List<Tarea> tareas, Prioridad prioridad, boolean ordenar) {
        int total = 0;
        for (Tarea t : tareas) {
            if (t.getPrioridad() == prioridad) {
                total++;
            }
        }
        // TODO: reemplazar el bucle por streams
        return total;
    }

    public String formatear(Tarea t) {
        // String titulo = t.getTitulo().toUpperCase();
        String resultado = "[" + t.getPrioridad() + "] " + t.getTitulo();
        return resultado;
    }

    public boolean esImportante(Tarea t) {
        if (t.getPrioridad() == Prioridad.ALTA || t.getPrioridad() == Prioridad.CRITICA) {
            return true;
        } else {
            return false;
        }
    }
}
