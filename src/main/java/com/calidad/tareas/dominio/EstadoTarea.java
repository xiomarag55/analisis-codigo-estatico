package com.calidad.tareas.dominio;

import java.util.Set;

/**
 * Máquina de estados de una tarea. Cada estado declara a cuáles puede pasar,
 * lo que evita un bloque if/else anidado (alta complejidad cognitiva).
 */
public enum EstadoTarea {
    PENDIENTE,
    EN_PROGRESO,
    COMPLETADA,
    CANCELADA;

    public boolean puedeTransicionarA(EstadoTarea destino) {
        return transicionesPermitidas().contains(destino);
    }

    private Set<EstadoTarea> transicionesPermitidas() {
        return switch (this) {
            case PENDIENTE -> Set.of(EN_PROGRESO, CANCELADA);
            case EN_PROGRESO -> Set.of(PENDIENTE, COMPLETADA, CANCELADA);
            case COMPLETADA, CANCELADA -> Set.of();
        };
    }

    public boolean esFinal() {
        return transicionesPermitidas().isEmpty();
    }
}
