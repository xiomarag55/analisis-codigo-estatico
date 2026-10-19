package com.calidad.tareas.legado;

import com.calidad.tareas.dominio.Prioridad;
import com.calidad.tareas.dominio.Tarea;
import java.time.LocalDate;
import java.util.List;

/**
 * CÓDIGO LEGADO (1), ya REFACTORIZADO a partir de los issues que reportó SonarQube.
 *
 * <p>Cada problema original y su corrección:
 * <ul>
 *   <li>java:S3776 / java:S1066: {@code resumen} tenía if anidados y complejidad cognitiva &gt; 15.
 *       Ahora usa un {@code switch} por estado y {@link Tarea#estaVencida(LocalDate)}.</li>
 *   <li>java:S1192: el literal "VENCIDA: " repetido pasó a la constante {@code VENCIDA}.</li>
 *   <li>java:S1659: cada contador se declara en su propia línea.</li>
 *   <li>java:S1172 / java:S1135: {@code contarPorPrioridad} perdió el parámetro sin usar y
 *       la tarea pendiente (pasar el bucle a streams) quedó resuelta.</li>
 *   <li>java:S125 / java:S1488: {@code formatear} sin código comentado ni variable intermedia.</li>
 *   <li>java:S1126: {@code esImportante} devuelve la expresión booleana directamente.</li>
 *   <li>java:S1068: se eliminó el campo privado sin usar {@code version}.</li>
 * </ul>
 * PMD lo excluye (ver pom.xml); las pruebas están en ReporteLegadoTest.
 */
public class ReporteLegado {

    private static final String VENCIDA = "VENCIDA: ";

    public String resumen(List<Tarea> tareas, LocalDate hoy, boolean incluirCanceladas) {
        StringBuilder texto = new StringBuilder();
        int pendientes = 0;
        int enProgreso = 0;
        int completadas = 0;
        int canceladas = 0;
        int vencidas = 0;
        for (Tarea t : tareas) {
            switch (t.getEstado()) {
                case PENDIENTE -> pendientes++;
                case EN_PROGRESO -> enProgreso++;
                case COMPLETADA -> completadas++;
                case CANCELADA -> canceladas++;
            }
            if (t.estaVencida(hoy)) {
                vencidas++;
                texto.append(VENCIDA).append(t.getTitulo()).append('\n');
            }
        }
        texto.append("Pendientes: ").append(pendientes)
                .append(", En progreso: ").append(enProgreso)
                .append(", Completadas: ").append(completadas);
        if (incluirCanceladas) {
            texto.append(", Canceladas: ").append(canceladas);
        }
        texto.append(", ").append(VENCIDA).append(vencidas);
        return texto.toString();
    }

    public int contarPorPrioridad(List<Tarea> tareas, Prioridad prioridad) {
        return (int) tareas.stream()
                .filter(t -> t.getPrioridad() == prioridad)
                .count();
    }

    public String formatear(Tarea t) {
        return "[" + t.getPrioridad() + "] " + t.getTitulo();
    }

    public boolean esImportante(Tarea t) {
        return t.getPrioridad() == Prioridad.ALTA || t.getPrioridad() == Prioridad.CRITICA;
    }
}
