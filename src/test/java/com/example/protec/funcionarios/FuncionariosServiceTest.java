package com.example.protec.funcionarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FuncionariosServiceTest {

    @Mock
    private FuncionariosRepository repository;

    @InjectMocks
    private FuncionariosService service;

    @Test
    void createSavesFuncionario() {
        when(repository.existsByNome("Carlos Silva")).thenReturn(false);

        FuncionariosResponse response = service.create("Carlos Silva");

        assertEquals("Carlos Silva", response.nome());
        verify(repository).save(any(Funcionarios.class));
    }

    @Test
    void createRejectsBlankNome() {
        assertThrows(IllegalArgumentException.class, () -> service.create(" "));
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsNullNome() {
        assertThrows(IllegalArgumentException.class, () -> service.create(null));
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateNome() {
        when(repository.existsByNome("Carlos Silva")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.create("Carlos Silva"));
        verify(repository, never()).save(any());
    }

    @Test
    void getByIdReturnsFuncionario() {
        UUID id = UUID.randomUUID();
        Funcionarios funcionario = new Funcionarios();
        funcionario.setNome("Carlos Silva");
        when(repository.findById(id)).thenReturn(Optional.of(funcionario));

        FuncionariosResponse response = service.getById(id);

        assertEquals("Carlos Silva", response.nome());
    }

    @Test
    void getByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getById(id));
    }

    @Test
    void getByNomeReturnsFuncionario() {
        Funcionarios funcionario = new Funcionarios();
        funcionario.setNome("Carlos Silva");
        when(repository.findByNome("Carlos Silva")).thenReturn(Optional.of(funcionario));

        FuncionariosResponse response = service.getByNome("Carlos Silva");

        assertEquals("Carlos Silva", response.nome());
    }

    @Test
    void getByNomeThrowsWhenMissing() {
        when(repository.findByNome("Inexistente")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getByNome("Inexistente"));
    }

    @Test
    void getAllReturnsList() {
        Funcionarios funcionario = new Funcionarios();
        funcionario.setNome("Carlos Silva");
        when(repository.findAll()).thenReturn(List.of(funcionario));

        List<FuncionariosResponse> list = service.getAll();

        assertEquals(1, list.size());
        assertEquals("Carlos Silva", list.get(0).nome());
    }

    @Test
    void updateNomeByIdChangesNome() {
        UUID id = UUID.randomUUID();
        Funcionarios funcionario = new Funcionarios();
        funcionario.setNome("Carlos");
        when(repository.existsByNome("Carlos Santos")).thenReturn(false);
        when(repository.findById(id)).thenReturn(Optional.of(funcionario));

        FuncionariosResponse response = service.updateNome(id, "Carlos Santos");

        assertEquals("Carlos Santos", response.nome());
    }

    @Test
    void updateNomeByIdRejectsNameAlreadyTaken() {
        when(repository.existsByNome("Carlos Santos")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.updateNome(UUID.randomUUID(), "Carlos Santos"));
    }

    @Test
    void updateNomeByNomeChangesNome() {
        Funcionarios funcionario = new Funcionarios();
        funcionario.setNome("Carlos");
        when(repository.existsByNome("Carlos Santos")).thenReturn(false);
        when(repository.findByNome("Carlos")).thenReturn(Optional.of(funcionario));

        FuncionariosResponse response = service.updateNome("Carlos", "Carlos Santos");

        assertEquals("Carlos Santos", response.nome());
    }

    @Test
    void deleteByIdDeletes() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(true);

        service.delete(id);

        verify(repository).deleteById(id);
    }

    @Test
    void deleteByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.delete(id));
        verify(repository, never()).deleteById(any());
    }

    @Test
    void deleteByNomeDeletes() {
        when(repository.existsByNome("Carlos")).thenReturn(true);

        service.delete("Carlos");

        verify(repository).deleteByNome("Carlos");
    }

    @Test
    void deleteByNomeThrowsWhenMissing() {
        when(repository.existsByNome("Carlos")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.delete("Carlos"));
        verify(repository, never()).deleteByNome(any());
    }
}
