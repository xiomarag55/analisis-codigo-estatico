package com.calidad.tareas.web;

import com.calidad.tareas.dominio.Prioridad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TareaRequest(
        @NotBlank @Size(max = 100) String titulo,
        @Size(max = 500) String descripcion,
        @NotNull Prioridad prioridad,
        LocalDate fechaLimite) {
}
