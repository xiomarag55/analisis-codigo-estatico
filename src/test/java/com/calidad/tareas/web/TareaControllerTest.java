package com.calidad.tareas.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.calidad.tareas.aplicacion.TareaService;
import com.calidad.tareas.dominio.EstadoTarea;
import com.calidad.tareas.dominio.Prioridad;
import com.calidad.tareas.dominio.TareaNoEncontradaException;
import com.calidad.tareas.dominio.TransicionInvalidaException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prueba "slice": solo la capa web (controlador + manejo de errores), con el servicio simulado.
 */
@WebMvcTest(TareaController.class)
class TareaControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TareaService service;

    private static TareaResponse respuesta(EstadoTarea estado) {
        return new TareaResponse(7L, "Pipeline", null, Prioridad.ALTA, estado, null, Instant.EPOCH, false, 30);
    }

    @Test
    void creaYDevuelveLocation() throws Exception {
        when(service.crear(any())).thenReturn(respuesta(EstadoTarea.PENDIENTE));

        mvc.perform(post("/api/v1/tareas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Pipeline\",\"prioridad\":\"ALTA\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/tareas/7"))
                .andExpect(jsonPath("$.urgencia").value(30));
    }

    @Test
    void rechazaPeticionInvalidaConDetalleDeCampos() throws Exception {
        mvc.perform(post("/api/v1/tareas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.errores.titulo").exists())
                .andExpect(jsonPath("$.errores.prioridad").exists());
    }

    @Test
    void devuelve404SiNoExiste() throws Exception {
        when(service.obtener(99L)).thenThrow(new TareaNoEncontradaException(99L));

        mvc.perform(get("/api/v1/tareas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe la tarea con id 99"));
    }

    @Test
    void devuelve409SiLaTransicionEsInvalida() throws Exception {
        when(service.cambiarEstado(7L, EstadoTarea.COMPLETADA))
                .thenThrow(new TransicionInvalidaException(EstadoTarea.PENDIENTE, EstadoTarea.COMPLETADA));

        mvc.perform(patch("/api/v1/tareas/7/estado").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"COMPLETADA\"}"))
                .andExpect(status().isConflict());
    }
}
