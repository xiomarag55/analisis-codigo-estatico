package com.calidad.tareas.web;

import com.calidad.tareas.dominio.EstadoTarea;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoRequest(@NotNull EstadoTarea estado) {
}
