package com.calidad.tareas.legado;

import com.calidad.tareas.dominio.EstadoTarea;
import com.calidad.tareas.dominio.Prioridad;
import com.calidad.tareas.dominio.Tarea;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CÓDIGO LEGADO (2), ya REFACTORIZADO a partir de los issues que reportó SonarQube.
 *
 * <p>Cada problema original y su corrección:
 * <ul>
 *   <li><b>Duplicación:</b> {@code exportarCsv} y {@code exportarTexto} repetían el mismo
 *       bloque de conteo. Ahora el texto reutiliza el CSV y los conteos salen de {@code contar}.</li>
 *   <li><b>Vulnerabilidad</b> java:S2068: la contraseña FTP estaba escrita en el código.
 *       Ahora se lee de la variable de entorno {@value #VARIABLE_PASSWORD_FTP}.</li>
 *   <li><b>Bugs</b> java:S2184: la división entera truncaba {@code porcentajeCompletadas};
 *       java:S2119: se creaba un {@code Random} nuevo en cada llamada (ahora uno compartido).</li>
 *   <li><b>Code smells:</b> System.out por logger, ternario anidado por {@code switch},
 *       variable sin usar eliminada y {@code catch (Exception)} acotado.</li>
 * </ul>
 * PMD lo excluye (ver pom.xml); las pruebas están en ExportadorLegadoTest.
 */
public class ExportadorLegado {

    static final String VARIABLE_PASSWORD_FTP = "EXPORTADOR_FTP_PASSWORD";

    private static final Logger LOG = LoggerFactory.getLogger(ExportadorLegado.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String FTP_USER = "exportador";

    private final String ftpPassword;

    public ExportadorLegado() {
        this(System.getenv().getOrDefault(VARIABLE_PASSWORD_FTP, ""));
    }

    ExportadorLegado(String ftpPassword) {
        this.ftpPassword = ftpPassword;
    }

    public String destinoFtp() {
        return "ftp://" + FTP_USER + ":" + ftpPassword + "@ftp.ejemplo.local/exportaciones";
    }

    public String exportarCsv(List<Tarea> tareas, LocalDate hoy) {
        StringBuilder salida = new StringBuilder();
        for (Tarea t : tareas) {
            salida.append(t.getId()).append(';')
                    .append(t.getTitulo()).append(';')
                    .append(t.getPrioridad()).append(';')
                    .append(t.getEstado()).append('\n');
        }
        long vencidas = tareas.stream().filter(t -> t.estaVencida(hoy)).count();
        salida.append("total;").append(tareas.size()).append('\n');
        salida.append("pendientes;").append(contar(tareas, EstadoTarea.PENDIENTE)).append('\n');
        salida.append("enProgreso;").append(contar(tareas, EstadoTarea.EN_PROGRESO)).append('\n');
        salida.append("completadas;").append(contar(tareas, EstadoTarea.COMPLETADA)).append('\n');
        salida.append("vencidas;").append(vencidas).append('\n');
        return salida.toString();
    }

    public String exportarTexto(List<Tarea> tareas, LocalDate hoy) {
        return exportarCsv(tareas, hoy).replace(';', ' ');
    }

    public double porcentajeCompletadas(List<Tarea> tareas) {
        if (tareas.isEmpty()) {
            return 0;
        }
        return contar(tareas, EstadoTarea.COMPLETADA) * 100.0 / tareas.size();
    }

    public String codigoExportacion() {
        int codigo = 1000 + RANDOM.nextInt(9000);
        LOG.info("Código de exportación generado: {}", codigo);
        return "EXP-" + codigo;
    }

    public String etiqueta(Tarea t) {
        return switch (t.getPrioridad()) {
            case CRITICA -> "!!!";
            case ALTA -> "!!";
            default -> "!";
        };
    }

    public Prioridad parsearPrioridad(String texto) {
        if (texto == null) {
            return Prioridad.MEDIA;
        }
        try {
            return Prioridad.valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Prioridad.MEDIA;
        }
    }

    private static long contar(List<Tarea> tareas, EstadoTarea estado) {
        return tareas.stream().filter(t -> t.getEstado() == estado).count();
    }
}
