package com.example.protec.maquinas;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class MaquinasControllerTest {

    @Mock
    private MaquinasService service;

    private MockMvc mvc;

    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new MaquinasController(service)).build();
    }

    // ── Getters ──

    @Test
    void listAllReturnsMachines() throws Exception {
        when(service.getAll()).thenReturn(List.of(new MaquinasResponse(id, "Prensa", "Esmagamento")));

        mvc.perform(get("/maquinas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Prensa"))
                .andExpect(jsonPath("$[0].riscos").value("Esmagamento"));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(service.getById(id)).thenThrow(new IllegalArgumentException("Maquina not found"));

        mvc.perform(get("/maquinas/{id}", id))
                .andExpect(status().isNotFound());
    }

    // ── Create ──

    @Test
    void createReturns201() throws Exception {
        when(service.create("Prensa", "Esmagamento"))
                .thenReturn(new MaquinasResponse(id, "Prensa", "Esmagamento"));

        mvc.perform(post("/maquinas")
                        .param("nome", "Prensa")
                        .param("riscos", "Esmagamento"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Prensa"));
    }

    @Test
    void createReturns400WhenServiceRejects() throws Exception {
        when(service.create("Prensa", "Esmagamento")).thenThrow(new IllegalArgumentException("nome already taken"));

        mvc.perform(post("/maquinas")
                        .param("nome", "Prensa")
                        .param("riscos", "Esmagamento"))
                .andExpect(status().isBadRequest());
    }

    // ── Update ──

    @Test
    void patchNomeUpdatesNome() throws Exception {
        when(service.updateNome(id, "Torno")).thenReturn(new MaquinasResponse(id, "Torno", "Esmagamento"));

        mvc.perform(patch("/maquinas/{id}/nome", id).param("nome", "Torno"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Torno"));
    }

    @Test
    void patchNomeReturns404WhenServiceRejects() throws Exception {
        when(service.updateNome(id, "Torno")).thenThrow(new IllegalArgumentException("Maquina not found"));

        mvc.perform(patch("/maquinas/{id}/nome", id).param("nome", "Torno"))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchRiscosUpdatesRiscos() throws Exception {
        when(service.updateRiscos(id, "Ruído")).thenReturn(new MaquinasResponse(id, "Prensa", "Ruído"));

        mvc.perform(patch("/maquinas/{id}/riscos", id).param("riscos", "Ruído"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riscos").value("Ruído"));
    }

    // ── Delete ──

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/maquinas/{id}", id))
                .andExpect(status().isNoContent());

        verify(service).delete(id);
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        doThrow(new IllegalArgumentException("maquina not found")).when(service).delete(id);

        mvc.perform(delete("/maquinas/{id}", id))
                .andExpect(status().isNotFound());
    }
}
