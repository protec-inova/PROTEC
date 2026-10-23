package com.example.protec.maquinas;

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
@Transactional // cada teste faz rollback
class MaquinasRepositoryTest {

    @Autowired
    private MaquinasRepository repository;

    private Maquinas maquina(String nome, String riscos) {
        Maquinas maquina = new Maquinas();
        maquina.setNome(nome);
        maquina.setRiscos(riscos);
        return maquina;
    }

    @Test
    void savesAndFindsByNome() {
        Maquinas saved = repository.saveAndFlush(maquina("Prensa", "Esmagamento de mãos"));

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals("Esmagamento de mãos", repository.findByNome("Prensa").orElseThrow().getRiscos());
        assertTrue(repository.existsByNome("Prensa"));
        assertFalse(repository.existsByNome("Torno"));
    }

    @Test
    void rejectsDuplicateNome() {
        repository.saveAndFlush(maquina("Serra", "Corte"));

        // a unicidade vem do índice criado no schema.sql
        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(maquina("Serra", "Outro risco")));
    }
}
