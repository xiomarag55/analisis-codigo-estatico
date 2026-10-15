package com.calidad.tareas.dominio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TareaRepository extends JpaRepository<Tarea, Long> {

    List<Tarea> findByEstado(EstadoTarea estado);
}
