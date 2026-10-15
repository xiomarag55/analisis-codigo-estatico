package com.calidad.tareas.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "tareas")
public class Tarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Prioridad prioridad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTarea estado = EstadoTarea.PENDIENTE;

    private LocalDate fechaLimite;

    @Column(nullable = false, updatable = false)
    private Instant creadaEn;

    protected Tarea() {
        // requerido por JPA
    }

    public Tarea(String titulo, String descripcion, Prioridad prioridad, LocalDate fechaLimite, Instant creadaEn) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.prioridad = prioridad;
        this.fechaLimite = fechaLimite;
        this.creadaEn = creadaEn;
    }

    public void actualizar(String titulo, String descripcion, Prioridad prioridad, LocalDate fechaLimite) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.prioridad = prioridad;
        this.fechaLimite = fechaLimite;
    }

    public void cambiarEstado(EstadoTarea destino) {
        if (!estado.puedeTransicionarA(destino)) {
            throw new TransicionInvalidaException(estado, destino);
        }
        this.estado = destino;
    }

    public boolean estaVencida(LocalDate hoy) {
        return fechaLimite != null && !estado.esFinal() && fechaLimite.isBefore(hoy);
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Prioridad getPrioridad() {
        return prioridad;
    }

    public EstadoTarea getEstado() {
        return estado;
    }

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }
}
