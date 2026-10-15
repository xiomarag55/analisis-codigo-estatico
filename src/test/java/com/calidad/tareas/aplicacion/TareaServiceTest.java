package com.calidad.tareas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.calidad.tareas.dominio.EstadoTarea;
import com.calidad.tareas.dominio.Prioridad;
import com.calidad.tareas.dominio.Tarea;
import com.calidad.tareas.dominio.TareaNoEncontradaException;
import com.calidad.tareas.dominio.TareaRepository;
import com.calidad.tareas.web.TareaRequest;
import com.calidad.tareas.web.TareaResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TareaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-15T10:00:00Z");

    @Mock
    private TareaRepository repository;

    private TareaService service;

    @BeforeEach
    void setUp() {
        service = new TareaService(repository, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private static Tarea tarea() {
        return new Tarea("Configurar pipeline", "CI/CD", Prioridad.ALTA, LocalDate.of(2026, 10, 20), AHORA);
    }

    @Test
    void listaTodasCuandoNoHayFiltro() {
        when(repository.findAll()).thenReturn(List.of(tarea()));

        assertThat(service.listar(null)).hasSize(1);
    }

    @Test
    void filtraPorEstado() {
        when(repository.findByEstado(EstadoTarea.PENDIENTE)).thenReturn(List.of(tarea(), tarea()));

        assertThat(service.listar(EstadoTarea.PENDIENTE)).hasSize(2);
    }

    @Test
    void creaUnaTareaConLaFechaDelReloj() {
        when(repository.save(any(Tarea.class))).thenAnswer(inv -> inv.getArgument(0));

        TareaResponse creada = service.crear(new TareaRequest("Nueva", null, Prioridad.BAJA, null));

        assertThat(creada.creadaEn()).isEqualTo(AHORA);
        assertThat(creada.estado()).isEqualTo(EstadoTarea.PENDIENTE);
    }

    @Test
    void actualizaUnaTareaExistente() {
        when(repository.findById(1L)).thenReturn(Optional.of(tarea()));

        TareaResponse actualizada = service.actualizar(1L, new TareaRequest("Otro", "d", Prioridad.CRITICA, null));

        assertThat(actualizada.titulo()).isEqualTo("Otro");
        assertThat(actualizada.prioridad()).isEqualTo(Prioridad.CRITICA);
    }

    @Test
    void cambiaElEstado() {
        when(repository.findById(1L)).thenReturn(Optional.of(tarea()));

        assertThat(service.cambiarEstado(1L, EstadoTarea.EN_PROGRESO).estado()).isEqualTo(EstadoTarea.EN_PROGRESO);
    }

    @Test
    void eliminaUnaTareaExistente() {
        Tarea existente = tarea();
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        service.eliminar(1L);

        verify(repository).delete(existente);
    }

    @Test
    void fallaSiLaTareaNoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L)).isInstanceOf(TareaNoEncontradaException.class);
    }
}
