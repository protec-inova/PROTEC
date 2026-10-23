package com.example.protec.consentimentos;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
class ConsentimentosControllerTest {

    @Mock
    private ConsentimentosService service;

    private MockMvc mvc;

    private final UUID funcionarioId = UUID.randomUUID();
    private final UUID maquinaId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ConsentimentosController(service)).build();
    }

    private ConsentimentosResponse response() {
        Instant now = Instant.now();
        return new ConsentimentosResponse(
                1, funcionarioId, maquinaId, "12345", "67890", now, now.plusSeconds(3600), true, 3600);
    }

    // ── Getters ──

    @Test
    void listAllReturnsConsentimentos() throws Exception {
        when(service.getAll()).thenReturn(List.of(response()));

        mvc.perform(get("/consentimentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].oculos").value("12345"))
                .andExpect(jsonPath("$[0].valido").value(true));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(service.getById(1)).thenThrow(new IllegalArgumentException("consentimento not found"));

        mvc.perform(get("/consentimentos/{id}", 1))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByFuncionarioReturnsHistory() throws Exception {
        when(service.getByFuncionario(funcionarioId, 0, 20)).thenReturn(List.of(response()));

        mvc.perform(get("/consentimentos/funcionario/{funcionarioId}", funcionarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].funcionarioId").value(funcionarioId.toString()));
    }

    @Test
    void getByFuncionarioReturns404WhenMissing() throws Exception {
        when(service.getByFuncionario(funcionarioId, 0, 20)).thenThrow(new IllegalArgumentException("funcionario not found"));

        mvc.perform(get("/consentimentos/funcionario/{funcionarioId}", funcionarioId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByFuncionarioPassesPageAndSize() throws Exception {
        when(service.getByFuncionario(funcionarioId, 2, 5)).thenReturn(List.of(response()));

        mvc.perform(get("/consentimentos/funcionario/{funcionarioId}", funcionarioId)
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].oculos").value("12345"));
    }

    @Test
    void getByMaquinaReturnsHistory() throws Exception {
        when(service.getByMaquina(maquinaId, 0, 20)).thenReturn(List.of(response()));

        mvc.perform(get("/consentimentos/maquina/{maquinaId}", maquinaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].maquinaId").value(maquinaId.toString()));
    }

    @Test
    void getByMaquinaReturns404WhenMissing() throws Exception {
        when(service.getByMaquina(maquinaId, 0, 20)).thenThrow(new IllegalArgumentException("maquina not found"));

        mvc.perform(get("/consentimentos/maquina/{maquinaId}", maquinaId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUltimoByFuncionarioReturnsConsentimento() throws Exception {
        when(service.getUltimoByFuncionario(funcionarioId)).thenReturn(response());

        mvc.perform(get("/consentimentos/ultimo")
                        .param("funcionarioId", funcionarioId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.segundosRestantes").value(3600));
    }

    @Test
    void getUltimoByMaquinaReturnsConsentimento() throws Exception {
        when(service.getUltimoByMaquina(maquinaId)).thenReturn(response());

        mvc.perform(get("/consentimentos/ultimo")
                        .param("maquinaId", maquinaId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maquinaId").value(maquinaId.toString()));
    }

    @Test
    void getUltimoReturns404WhenMissing() throws Exception {
        when(service.getUltimoByMaquina(maquinaId))
                .thenThrow(new IllegalArgumentException("consentimento not found"));

        mvc.perform(get("/consentimentos/ultimo")
                        .param("maquinaId", maquinaId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUltimoReturns400WhenBothParamsSent() throws Exception {
        mvc.perform(get("/consentimentos/ultimo")
                        .param("funcionarioId", funcionarioId.toString())
                        .param("maquinaId", maquinaId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUltimoReturns400WhenNoParamSent() throws Exception {
        mvc.perform(get("/consentimentos/ultimo"))
                .andExpect(status().isBadRequest());
    }

    // ── Create ──

    @Test
    void createReturns201() throws Exception {
        when(service.create(funcionarioId, maquinaId, "12345", "67890")).thenReturn(response());

        mvc.perform(post("/consentimentos")
                        .param("funcionarioId", funcionarioId.toString())
                        .param("maquinaId", maquinaId.toString())
                        .param("oculos", "12345")
                        .param("bota", "67890"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bota").value("67890"));
    }

    @Test
    void createReturns400WhenServiceRejects() throws Exception {
        when(service.create(funcionarioId, maquinaId, "12345", "67890"))
                .thenThrow(new IllegalArgumentException("funcionario not found"));

        mvc.perform(post("/consentimentos")
                        .param("funcionarioId", funcionarioId.toString())
                        .param("maquinaId", maquinaId.toString())
                        .param("oculos", "12345")
                        .param("bota", "67890"))
                .andExpect(status().isBadRequest());
    }
}