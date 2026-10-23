package com.example.protec.funcionarios;

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
class FuncionariosControllerTest {

    @Mock
    private FuncionariosService service;

    private MockMvc mvc;

    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new FuncionariosController(service)).build();
    }

    // ── Getters ──

    @Test
    void listAllReturnsFuncionarios() throws Exception {
        when(service.getAll()).thenReturn(List.of(new FuncionariosResponse(id, "Carlos Silva")));

        mvc.perform(get("/funcionarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Carlos Silva"));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(service.getById(id)).thenThrow(new IllegalArgumentException("funcionario not found"));

        mvc.perform(get("/funcionarios/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByNomeReturnsFuncionario() throws Exception {
        when(service.getByNome("Carlos")).thenReturn(new FuncionariosResponse(id, "Carlos"));

        mvc.perform(get("/funcionarios/nome/{nome}", "Carlos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Carlos"));
    }

    @Test
    void getByNomeReturns404WhenMissing() throws Exception {
        when(service.getByNome("Inexistente")).thenThrow(new IllegalArgumentException("funcionario not found"));

        mvc.perform(get("/funcionarios/nome/{nome}", "Inexistente"))
                .andExpect(status().isNotFound());
    }

    // ── Create ──

    @Test
    void createReturns201() throws Exception {
        when(service.create("Carlos Silva"))
                .thenReturn(new FuncionariosResponse(id, "Carlos Silva"));

        mvc.perform(post("/funcionarios")
                        .param("nome", "Carlos Silva"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Carlos Silva"));
    }

    @Test
    void createReturns400WhenServiceRejects() throws Exception {
        when(service.create("Carlos Silva")).thenThrow(new IllegalArgumentException("nome already taken"));

        mvc.perform(post("/funcionarios")
                        .param("nome", "Carlos Silva"))
                .andExpect(status().isBadRequest());
    }

    // ── Update ──

    @Test
    void patchNomeByIdUpdatesNome() throws Exception {
        when(service.updateNome(id, "Carlos Santos")).thenReturn(new FuncionariosResponse(id, "Carlos Santos"));

        mvc.perform(patch("/funcionarios/{id}/nome", id).param("nome", "Carlos Santos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Carlos Santos"));
    }

    @Test
    void patchNomeByIdReturns404WhenNotFound() throws Exception {
        when(service.updateNome(id, "Carlos Santos")).thenThrow(new IllegalArgumentException("funcionario not found"));

        mvc.perform(patch("/funcionarios/{id}/nome", id).param("nome", "Carlos Santos"))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchNomeByNomeUpdatesNome() throws Exception {
        when(service.updateNome("Carlos", "Carlos Santos")).thenReturn(new FuncionariosResponse(id, "Carlos Santos"));

        mvc.perform(patch("/funcionarios/nome/{nome}/nome", "Carlos").param("newNome", "Carlos Santos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Carlos Santos"));
    }

    // ── Delete ──

    @Test
    void deleteByIdReturns204() throws Exception {
        mvc.perform(delete("/funcionarios/{id}", id))
                .andExpect(status().isNoContent());

        verify(service).delete(id);
    }

    @Test
    void deleteByIdReturns404WhenMissing() throws Exception {
        doThrow(new IllegalArgumentException("funcionario not found")).when(service).delete(id);

        mvc.perform(delete("/funcionarios/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteByNomeReturns204() throws Exception {
        mvc.perform(delete("/funcionarios/nome/{nome}", "Carlos"))
                .andExpect(status().isNoContent());

        verify(service).delete("Carlos");
    }

    @Test
    void deleteByNomeReturns404WhenMissing() throws Exception {
        doThrow(new IllegalArgumentException("funcionario not found")).when(service).delete("Carlos");

        mvc.perform(delete("/funcionarios/nome/{nome}", "Carlos"))
                .andExpect(status().isNotFound());
    }
}
