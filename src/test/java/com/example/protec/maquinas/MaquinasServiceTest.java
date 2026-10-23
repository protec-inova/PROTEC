package com.example.protec.maquinas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MaquinasServiceTest {

    @Mock
    private MaquinasRepository repository;

    @InjectMocks
    private MaquinasService service;

    @Test
    void createSavesMachine() {
        when(repository.existsByNome("Prensa")).thenReturn(false);

        MaquinasResponse response = service.create("Prensa", "Esmagamento");

        assertEquals("Prensa", response.nome());
        assertEquals("Esmagamento", response.riscos());
        verify(repository).save(any(Maquinas.class));
    }

    @Test
    void createRejectsBlankNome() {
        assertThrows(IllegalArgumentException.class, () -> service.create(" ", "Esmagamento"));
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsBlankRiscos() {
        assertThrows(IllegalArgumentException.class, () -> service.create("Prensa", ""));
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateNome() {
        when(repository.existsByNome("Prensa")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.create("Prensa", "Esmagamento"));
        verify(repository, never()).save(any());
    }

    @Test
    void getByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getById(id));
    }

    @Test
    void updateNomeChangesNome() {
        UUID id = UUID.randomUUID();
        Maquinas maquina = new Maquinas();
        maquina.setNome("Prensa");
        maquina.setRiscos("Esmagamento");
        when(repository.existsByNome("Torno")).thenReturn(false);
        when(repository.findById(id)).thenReturn(Optional.of(maquina));

        MaquinasResponse response = service.updateNome(id, "Torno");

        assertEquals("Torno", response.nome());
        assertEquals("Esmagamento", response.riscos());
    }

    @Test
    void updateNomeRejectsNameAlreadyTaken() {
        when(repository.existsByNome("Torno")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.updateNome(UUID.randomUUID(), "Torno"));
    }

    @Test
    void updateRiscosChangesOnlyRiscos() {
        UUID id = UUID.randomUUID();
        Maquinas maquina = new Maquinas();
        maquina.setNome("Prensa");
        maquina.setRiscos("Esmagamento");
        when(repository.findById(id)).thenReturn(Optional.of(maquina));

        MaquinasResponse response = service.updateRiscos(id, "Ruído");

        assertEquals("Prensa", response.nome());
        assertEquals("Ruído", response.riscos());
    }

    @Test
    void deleteThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.delete(id));
        verify(repository, never()).deleteById(any());
    }
}
