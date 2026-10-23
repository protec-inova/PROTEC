package com.example.protec.funcionarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FuncionariosRepositoryTest {

    @Autowired
    private FuncionariosRepository repository;

    private Funcionarios funcionario(String nome) {
        Funcionarios funcionario = new Funcionarios();
        funcionario.setNome(nome);
        return funcionario;
    }

    @Test
    void savesAndFindsByNome() {
        Funcionarios saved = repository.saveAndFlush(funcionario("Carlos Silva"));

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals("Carlos Silva", repository.findByNome("Carlos Silva").orElseThrow().getNome());
        assertTrue(repository.existsByNome("Carlos Silva"));
        assertFalse(repository.existsByNome("Ana Souza"));
    }

    @Test
    void rejectsDuplicateNome() {
        repository.saveAndFlush(funcionario("Carlos Silva"));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(funcionario("Carlos Silva")));
    }
}
