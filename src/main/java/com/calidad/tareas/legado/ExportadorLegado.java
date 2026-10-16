package com.calidad.tareas.legado;

import com.calidad.tareas.dominio.EstadoTarea;
import com.calidad.tareas.dominio.Prioridad;
import com.calidad.tareas.dominio.Tarea;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

/**
 * CÓDIGO LEGADO INTENCIONAL (2), para practicar el análisis en SonarQube.
 *
 * <p>Complementa a {@link ReporteLegado} con problemas de OTROS tipos, para que los
 * tableros de SonarQube tengan datos en todas las categorías:
 * <ul>
 *   <li><b>Duplicación:</b> {@code exportarCsv} y {@code exportarTexto} repiten el mismo
 *       bloque de conteo (copiar y pegar). Refactor: extraer un método común.</li>
 *   <li><b>Vulnerabilidad (seguridad)</b> java:S2068: contraseña escrita en el código ({@code FTP_PASSWORD}).
 *       Refactor: leerla de una variable de entorno.</li>
 *   <li><b>Bugs (fiabilidad)</b> java:S2184: división entera asignada a un double en
 *       {@code porcentajeCompletadas}; java:S2119: se crea un {@code Random} nuevo en cada llamada.</li>
 *   <li><b>Code smells:</b> java:S106 (System.out), java:S3358 (ternario anidado),
 *       java:S1481 y java:S1854 (variable sin usar), java:S1488 (variable devuelta de inmediato).</li>
 * </ul>
 * PMD lo excluye (ver pom.xml); las pruebas están en ExportadorLegadoTest.
 */
public class ExportadorLegado {

    private static final String FTP_USER = "exportador";
    private static final String FTP_PASSWORD = "Export2024!";

    public String destinoFtp() {
        return "ftp://" + FTP_USER + ":" + FTP_PASSWORD + "@ftp.ejemplo.local/exportaciones";
    }

    public String exportarCsv(List<Tarea> tareas, LocalDate hoy) {
        StringBuilder salida = new StringBuilder();
        int pendientes = 0;
        int enProgreso = 0;
        int completadas = 0;
        int vencidas = 0;
        for (Tarea t : tareas) {
            if (t.getEstado() == EstadoTarea.PENDIENTE) {
                pendientes++;
            }
            if (t.getEstado() == EstadoTarea.EN_PROGRESO) {
                enProgreso++;
            }
            if (t.getEstado() == EstadoTarea.COMPLETADA) {
                completadas++;
            }
            if (t.estaVencida(hoy)) {
                vencidas++;
            }
            salida.append(t.getId()).append(';')
                    .append(t.getTitulo()).append(';')
                    .append(t.getPrioridad()).append(';')
                    .append(t.getEstado()).append('\n');
        }
        salida.append("total;").append(tareas.size()).append('\n');
        salida.append("pendientes;").append(pendientes).append('\n');
        salida.append("enProgreso;").append(enProgreso).append('\n');
        salida.append("completadas;").append(completadas).append('\n');
        salida.append("vencidas;").append(vencidas).append('\n');
        return salida.toString();
    }

    public String exportarTexto(List<Tarea> tareas, LocalDate hoy) {
        StringBuilder salida = new StringBuilder();
        int pendientes = 0;
        int enProgreso = 0;
        int completadas = 0;
        int vencidas = 0;
        for (Tarea t : tareas) {
            if (t.getEstado() == EstadoTarea.PENDIENTE) {
                pendientes++;
            }
            if (t.getEstado() == EstadoTarea.EN_PROGRESO) {
                enProgreso++;
            }
            if (t.getEstado() == EstadoTarea.COMPLETADA) {
                completadas++;
            }
            if (t.estaVencida(hoy)) {
                vencidas++;
            }
            salida.append(t.getId()).append(';')
                    .append(t.getTitulo()).append(';')
                    .append(t.getPrioridad()).append(';')
                    .append(t.getEstado()).append('\n');
        }
        salida.append("total;").append(tareas.size()).append('\n');
        salida.append("pendientes;").append(pendientes).append('\n');
        salida.append("enProgreso;").append(enProgreso).append('\n');
        salida.append("completadas;").append(completadas).append('\n');
        salida.append("vencidas;").append(vencidas).append('\n');
        return salida.toString().replace(';', ' ');
    }

    public double porcentajeCompletadas(List<Tarea> tareas) {
        if (tareas.isEmpty()) {
            return 0;
        }
        int completadas = 0;
        for (Tarea t : tareas) {
            if (t.getEstado() == EstadoTarea.COMPLETADA) {
                completadas++;
            }
        }
        double porcentaje = completadas * 100 / tareas.size();
        return porcentaje;
    }

    public String codigoExportacion() {
        Random random = new Random();
        int codigo = 1000 + random.nextInt(9000);
        System.out.println("Código de exportación generado: " + codigo);
        return "EXP-" + codigo;
    }

    public String etiqueta(Tarea t) {
        int largo = t.getTitulo().length();
        return t.getPrioridad() == Prioridad.CRITICA ? "!!!" : t.getPrioridad() == Prioridad.ALTA ? "!!" : "!";
    }

    public Prioridad parsearPrioridad(String texto) {
        try {
            return Prioridad.valueOf(texto.trim().toUpperCase());
        } catch (Exception e) {
            return Prioridad.MEDIA;
        }
    }
}
