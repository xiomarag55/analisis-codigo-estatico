package com.calidad.tareas.web;

import com.calidad.tareas.aplicacion.TareaService;
import com.calidad.tareas.dominio.EstadoTarea;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tareas")
@Tag(name = "Tareas", description = "Gestión de tareas y de su flujo de estados")
public class TareaController {

    private final TareaService service;

    public TareaController(TareaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar tareas", description = "Devuelve todas las tareas o solo las del estado indicado")
    public List<TareaResponse> listar(
            @Parameter(description = "Filtra por estado") @RequestParam(required = false) EstadoTarea estado) {
        return service.listar(estado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una tarea")
    @ApiResponse(responseCode = "200", description = "Tarea encontrada")
    @ApiResponse(responseCode = "404", description = "No existe la tarea")
    public TareaResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Crear una tarea", description = "La tarea nace en estado PENDIENTE")
    @ApiResponse(responseCode = "201", description = "Tarea creada; la cabecera Location apunta a ella")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    public ResponseEntity<TareaResponse> crear(@Valid @RequestBody TareaRequest request) {
        TareaResponse creada = service.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/tareas/" + creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar los datos de una tarea")
    @ApiResponse(responseCode = "200", description = "Tarea actualizada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "No existe la tarea")
    public TareaResponse actualizar(@PathVariable Long id, @Valid @RequestBody TareaRequest request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar el estado de una tarea",
            description = "PENDIENTE → EN_PROGRESO | CANCELADA; EN_PROGRESO → PENDIENTE | COMPLETADA | CANCELADA. "
                    + "COMPLETADA y CANCELADA son estados finales.")
    @ApiResponse(responseCode = "200", description = "Estado cambiado")
    @ApiResponse(responseCode = "404", description = "No existe la tarea")
    @ApiResponse(responseCode = "409", description = "Transición de estado no permitida")
    public TareaResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoRequest request) {
        return service.cambiarEstado(id, request.estado());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una tarea")
    @ApiResponse(responseCode = "204", description = "Tarea eliminada")
    @ApiResponse(responseCode = "404", description = "No existe la tarea")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
