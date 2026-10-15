package com.calidad.tareas.dominio;

public class TransicionInvalidaException extends RuntimeException {

    public TransicionInvalidaException(EstadoTarea origen, EstadoTarea destino) {
        super("No se puede pasar una tarea de %s a %s".formatted(origen, destino));
    }
}
