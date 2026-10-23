package com.example.protec.consentimentos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentimentosRepository extends JpaRepository<Consentimentos, Integer> {
    List<Consentimentos> findByFuncionarioIdOrderByDataDesc(UUID funcionarioId, Pageable pageable);

    List<Consentimentos> findByMaquinaIdOrderByDataDesc(UUID maquinaId, Pageable pageable);

    Optional<Consentimentos> findFirstByFuncionarioIdOrderByDataDesc(UUID funcionarioId);

    Optional<Consentimentos> findFirstByMaquinaIdOrderByDataDesc(UUID maquinaId);
}