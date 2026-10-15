package com.calidad.tareas.web;

import com.calidad.tareas.dominio.CalculadoraUrgencia;
import com.calidad.tareas.dominio.EstadoTarea;
import com.calidad.tareas.dominio.Prioridad;
import com.calidad.tareas.dominio.Tarea;
import java.time.Instant;
import java.time.LocalDate;

public record TareaResponse(
        Long id,
        String titulo,
        String descripcion,
        Prioridad prioridad,
        EstadoTarea estado,
        LocalDate fechaLimite,
        Instant creadaEn,
        boolean vencida,
        int urgencia) {

    public static TareaResponse desde(Tarea tarea, LocalDate hoy) {
        return new TareaResponse(
                tarea.getId(),
                tarea.getTitulo(),
                tarea.getDescripcion(),
                tarea.getPrioridad(),
                tarea.getEstado(),
                tarea.getFechaLimite(),
                tarea.getCreadaEn(),
                tarea.estaVencida(hoy),
                CalculadoraUrgencia.calcular(tarea, hoy));
    }
}
