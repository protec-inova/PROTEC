package com.example.protec.consentimentos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.protec.funcionarios.FuncionariosRepository;
import com.example.protec.maquinas.MaquinasRepository;

@ExtendWith(MockitoExtension.class)
class ConsentimentosServiceTest {

    @Mock
    private ConsentimentosRepository repository;

    @Mock
    private FuncionariosRepository funcionariosRepository;

    @Mock
    private MaquinasRepository maquinasRepository;

    @InjectMocks
    private ConsentimentosService service;

    private final UUID funcionarioId = UUID.randomUUID();
    private final UUID maquinaId = UUID.randomUUID();

    private Consentimentos consentimentoDe(Duration haQuantoTempo) {
        Consentimentos consentimento = new Consentimentos();
        consentimento.setFuncionarioId(funcionarioId);
        consentimento.setMaquinaId(maquinaId);
        ReflectionTestUtils.setField(consentimento, "data", Instant.now().minus(haQuantoTempo));
        return consentimento;
    }

    @Test
    void createSavesConsentimento() {
        when(funcionariosRepository.existsById(funcionarioId)).thenReturn(true);
        when(maquinasRepository.existsById(maquinaId)).thenReturn(true);
        when(repository.save(any(Consentimentos.class))).thenAnswer(invocation -> {
            Consentimentos consentimento = invocation.getArgument(0);
            consentimento.onCreate(); // simula o @PrePersist
            return consentimento;
        });

        ConsentimentosResponse response = service.create(funcionarioId, maquinaId, "12345", "67890");

        assertEquals("12345", response.oculos());
        assertEquals("67890", response.bota());
        assertTrue(response.valido());
        verify(repository).save(any(Consentimentos.class));
    }

    @Test
    void createTurnsBlankCaIntoNull() {
        when(funcionariosRepository.existsById(funcionarioId)).thenReturn(true);
        when(maquinasRepository.existsById(maquinaId)).thenReturn(true);
        when(repository.save(any(Consentimentos.class))).thenAnswer(invocation -> {
            Consentimentos consentimento = invocation.getArgument(0);
            consentimento.onCreate();
            return consentimento;
        });

        ConsentimentosResponse response = service.create(funcionarioId, maquinaId, " ", null);

        assertNull(response.oculos());
        assertNull(response.bota());
    }

    @Test
    void createRejectsUnknownFuncionario() {
        when(funcionariosRepository.existsById(funcionarioId)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> service.create(funcionarioId, maquinaId, "12345", "67890"));
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsUnknownMaquina() {
        when(funcionariosRepository.existsById(funcionarioId)).thenReturn(true);
        when(maquinasRepository.existsById(maquinaId)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> service.create(funcionarioId, maquinaId, "12345", "67890"));
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsTooLongCa() {
        when(funcionariosRepository.existsById(funcionarioId)).thenReturn(true);
        when(maquinasRepository.existsById(maquinaId)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.create(funcionarioId, maquinaId, "1".repeat(21), "67890"));
        verify(repository, never()).save(any());
    }

    @Test
    void getUltimoByFuncionarioIsValidWithin1Hour() {
        when(repository.findFirstByFuncionarioIdOrderByDataDesc(funcionarioId))
                .thenReturn(Optional.of(consentimentoDe(Duration.ofMinutes(59))));

        ConsentimentosResponse response = service.getUltimoByFuncionario(funcionarioId);

        assertTrue(response.valido());
        assertTrue(response.segundosRestantes() > 0);
    }

    @Test
    void getUltimoByFuncionarioIsExpiredAfter1Hour() {
        when(repository.findFirstByFuncionarioIdOrderByDataDesc(funcionarioId))
                .thenReturn(Optional.of(consentimentoDe(Duration.ofMinutes(61))));

        ConsentimentosResponse response = service.getUltimoByFuncionario(funcionarioId);

        assertFalse(response.valido());
        assertEquals(0, response.segundosRestantes());
    }

    @Test
    void getUltimoByFuncionarioThrowsWhenMissing() {
        when(repository.findFirstByFuncionarioIdOrderByDataDesc(funcionarioId))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getUltimoByFuncionario(funcionarioId));
    }

    @Test
    void getUltimoByMaquinaIsValidWithin1Hour() {
        when(repository.findFirstByMaquinaIdOrderByDataDesc(maquinaId))
                .thenReturn(Optional.of(consentimentoDe(Duration.ofMinutes(59))));

        ConsentimentosResponse response = service.getUltimoByMaquina(maquinaId);

        assertTrue(response.valido());
        assertEquals(maquinaId, response.maquinaId());
    }

    @Test
    void getUltimoByMaquinaIsExpiredAfter1Hour() {
        when(repository.findFirstByMaquinaIdOrderByDataDesc(maquinaId))
                .thenReturn(Optional.of(consentimentoDe(Duration.ofMinutes(61))));

        ConsentimentosResponse response = service.getUltimoByMaquina(maquinaId);

        assertFalse(response.valido());
        assertEquals(0, response.segundosRestantes());
    }

    @Test
    void getUltimoByMaquinaThrowsWhenMissing() {
        when(repository.findFirstByMaquinaIdOrderByDataDesc(maquinaId))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getUltimoByMaquina(maquinaId));
    }

    @Test
    void getByFuncionarioReturnsHistory() {
        when(funcionariosRepository.existsById(funcionarioId)).thenReturn(true);
        when(repository.findByFuncionarioIdOrderByDataDesc(funcionarioId, PageRequest.of(0, 20)))
                .thenReturn(List.of(consentimentoDe(Duration.ofMinutes(5)), consentimentoDe(Duration.ofHours(2))));

        List<ConsentimentosResponse> response = service.getByFuncionario(funcionarioId, 0, 20);

        assertEquals(2, response.size());
        assertTrue(response.get(0).valido());
        assertFalse(response.get(1).valido());
    }

    @Test
    void getByFuncionarioThrowsWhenFuncionarioMissing() {
        when(funcionariosRepository.existsById(funcionarioId)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.getByFuncionario(funcionarioId, 0, 20));
        verify(repository, never()).findByFuncionarioIdOrderByDataDesc(any(), any());
    }

    @Test
    void getByMaquinaReturnsHistory() {
        when(maquinasRepository.existsById(maquinaId)).thenReturn(true);
        when(repository.findByMaquinaIdOrderByDataDesc(maquinaId, PageRequest.of(0, 20)))
                .thenReturn(List.of(consentimentoDe(Duration.ofMinutes(5))));

        List<ConsentimentosResponse> response = service.getByMaquina(maquinaId, 0, 20);

        assertEquals(1, response.size());
        assertEquals(maquinaId, response.get(0).maquinaId());
    }

    @Test
    void getByMaquinaClampsPageAndSize() {
        when(maquinasRepository.existsById(maquinaId)).thenReturn(true);
        when(repository.findByMaquinaIdOrderByDataDesc(maquinaId, PageRequest.of(0, 100)))
                .thenReturn(List.of());

        service.getByMaquina(maquinaId, -5, 5000);

        verify(repository).findByMaquinaIdOrderByDataDesc(maquinaId, PageRequest.of(0, 100));
    }

    @Test
    void getByMaquinaThrowsWhenMaquinaMissing() {
        when(maquinasRepository.existsById(maquinaId)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.getByMaquina(maquinaId, 0, 20));
        verify(repository, never()).findByMaquinaIdOrderByDataDesc(any(), any());
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(repository.findById(1)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getById(1));
    }
}