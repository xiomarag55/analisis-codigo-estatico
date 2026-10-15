package com.calidad.tareas;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.calidad.tareas.dominio.TareaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prueba de INTEGRACIÓN (Failsafe, sufijo IT): contexto Spring completo + base de datos H2 real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TareasApiIT {

    private static final String API = "/api/v1/tareas";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TareaRepository repository;

    @BeforeEach
    void limpiar() {
        repository.deleteAll();
    }

    private String crear(String json) throws Exception {
        return mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
    }

    @Test
    void cicloDeVidaCompletoDeUnaTarea() throws Exception {
        String url = crear("{\"titulo\":\"Configurar Sonar\",\"prioridad\":\"ALTA\"}");

        mvc.perform(get(url)).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.urgencia").value(30));

        mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Configurar SonarQube\",\"prioridad\":\"CRITICA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Configurar SonarQube"));

        mvc.perform(patch(url + "/estado").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_PROGRESO\"}"))
                .andExpect(status().isOk());

        mvc.perform(patch(url + "/estado").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"COMPLETADA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urgencia").value(0));

        mvc.perform(delete(url)).andExpect(status().isNoContent());
        mvc.perform(get(url)).andExpect(status().isNotFound());
    }

    @Test
    void filtraPorEstado() throws Exception {
        crear("{\"titulo\":\"A\",\"prioridad\":\"BAJA\"}");
        String url = crear("{\"titulo\":\"B\",\"prioridad\":\"MEDIA\"}");
        mvc.perform(patch(url + "/estado").contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"CANCELADA\"}"));

        mvc.perform(get(API)).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get(API).param("estado", "CANCELADA")).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void exponeSaludEInformacionDeVersion() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/info")).andExpect(status().isOk())
                .andExpect(jsonPath("$.build.artifact").value("tareas-api"));
    }
}
