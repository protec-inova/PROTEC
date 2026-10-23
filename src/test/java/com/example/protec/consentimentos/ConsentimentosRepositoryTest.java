package com.example.protec.consentimentos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.protec.funcionarios.Funcionarios;
import com.example.protec.funcionarios.FuncionariosRepository;
import com.example.protec.maquinas.Maquinas;
import com.example.protec.maquinas.MaquinasRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional // cada teste faz rollback
class ConsentimentosRepositoryTest {

    @Autowired
    private ConsentimentosRepository repository;

    @Autowired
    private FuncionariosRepository funcionariosRepository;

    @Autowired
    private MaquinasRepository maquinasRepository;

    private UUID funcionarioId() {
        Funcionarios funcionario = new Funcionarios();
        funcionario.setNome("Carlos");
        return funcionariosRepository.saveAndFlush(funcionario).getId();
    }

    private UUID maquinaId() {
        Maquinas maquina = new Maquinas();
        maquina.setNome("Prensa");
        maquina.setRiscos("Esmagamento");
        return maquinasRepository.saveAndFlush(maquina).getId();
    }

    private Consentimentos consentimento(UUID funcionarioId, UUID maquinaId) {
        Consentimentos consentimento = new Consentimentos();
        consentimento.setFuncionarioId(funcionarioId);
        consentimento.setMaquinaId(maquinaId);
        consentimento.setOculos("12345");
        consentimento.setBota("67890");
        return consentimento;
    }

    @Test
    void savesAndFindsUltimo() {
        UUID funcionarioId = funcionarioId();
        UUID maquinaId = maquinaId();

        Consentimentos saved = repository.saveAndFlush(consentimento(funcionarioId, maquinaId));

        assertNotNull(saved.getId());
        assertNotNull(saved.getData());
        Consentimentos byFuncionario = repository
                .findFirstByFuncionarioIdOrderByDataDesc(funcionarioId)
                .orElseThrow();
        Consentimentos byMaquina = repository
                .findFirstByMaquinaIdOrderByDataDesc(maquinaId)
                .orElseThrow();
        assertEquals("12345", byFuncionario.getOculos());
        assertEquals("67890", byMaquina.getBota());
    }

    @Test
    void findsByFuncionarioAndByMaquina() {
        UUID funcionarioId = funcionarioId();
        UUID maquinaId = maquinaId();
        repository.saveAndFlush(consentimento(funcionarioId, maquinaId));

        Pageable pagina = PageRequest.of(0, 10);

        assertEquals(1, repository.findByFuncionarioIdOrderByDataDesc(funcionarioId, pagina).size());
        assertEquals(1, repository.findByMaquinaIdOrderByDataDesc(maquinaId, pagina).size());
        assertTrue(repository.findByFuncionarioIdOrderByDataDesc(UUID.randomUUID(), pagina).isEmpty());
        assertTrue(repository.findByMaquinaIdOrderByDataDesc(UUID.randomUUID(), pagina).isEmpty());
    }

    @Test
    void paginatesHistory() {
        UUID funcionarioId = funcionarioId();
        UUID maquinaId = maquinaId();
        repository.saveAndFlush(consentimento(funcionarioId, maquinaId));
        repository.saveAndFlush(consentimento(funcionarioId, maquinaId));
        repository.saveAndFlush(consentimento(funcionarioId, maquinaId));

        assertEquals(2, repository.findByFuncionarioIdOrderByDataDesc(funcionarioId, PageRequest.of(0, 2)).size());
        assertEquals(1, repository.findByFuncionarioIdOrderByDataDesc(funcionarioId, PageRequest.of(1, 2)).size());
    }

    @Test
    void ultimoIsEmptyWhenNoConsentimento() {
        assertTrue(repository.findFirstByFuncionarioIdOrderByDataDesc(UUID.randomUUID()).isEmpty());
        assertTrue(repository.findFirstByMaquinaIdOrderByDataDesc(UUID.randomUUID()).isEmpty());
    }

    @Test
    void rejectsUnknownFuncionario() {
        UUID maquinaId = maquinaId();

        // a FK vem do schema.sql
        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(consentimento(UUID.randomUUID(), maquinaId)));
    }
}