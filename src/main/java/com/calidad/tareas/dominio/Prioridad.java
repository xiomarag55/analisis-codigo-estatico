package com.calidad.tareas.dominio;

public enum Prioridad {
    BAJA(1),
    MEDIA(2),
    ALTA(3),
    CRITICA(5);

    private final int peso;

    Prioridad(int peso) {
        this.peso = peso;
    }

    public int peso() {
        return peso;
    }
}
