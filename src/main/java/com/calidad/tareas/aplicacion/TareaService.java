package com.calidad.tareas.aplicacion;

import com.calidad.tareas.dominio.EstadoTarea;
import com.calidad.tareas.dominio.Tarea;
import com.calidad.tareas.dominio.TareaNoEncontradaException;
import com.calidad.tareas.dominio.TareaRepository;
import com.calidad.tareas.web.TareaRequest;
import com.calidad.tareas.web.TareaResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TareaService {

    private final TareaRepository repository;
    private final Clock clock;

    public TareaService(TareaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<TareaResponse> listar(EstadoTarea estado) {
        List<Tarea> tareas = estado == null ? repository.findAll() : repository.findByEstado(estado);
        LocalDate hoy = hoy();
        return tareas.stream().map(t -> TareaResponse.desde(t, hoy)).toList();
    }

    @Transactional(readOnly = true)
    public TareaResponse obtener(Long id) {
        return TareaResponse.desde(buscar(id), hoy());
    }

    public TareaResponse crear(TareaRequest request) {
        Tarea tarea = new Tarea(request.titulo(), request.descripcion(), request.prioridad(),
                request.fechaLimite(), Instant.now(clock));
        return TareaResponse.desde(repository.save(tarea), hoy());
    }

    public TareaResponse actualizar(Long id, TareaRequest request) {
        Tarea tarea = buscar(id);
        tarea.actualizar(request.titulo(), request.descripcion(), request.prioridad(), request.fechaLimite());
        return TareaResponse.desde(tarea, hoy());
    }

    public TareaResponse cambiarEstado(Long id, EstadoTarea destino) {
        Tarea tarea = buscar(id);
        tarea.cambiarEstado(destino);
        return TareaResponse.desde(tarea, hoy());
    }

    public void eliminar(Long id) {
        repository.delete(buscar(id));
    }

    private Tarea buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new TareaNoEncontradaException(id));
    }

    private LocalDate hoy() {
        return LocalDate.now(clock);
    }
}
