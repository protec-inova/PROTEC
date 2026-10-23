package com.example.protec.maquinas;

import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface MaquinasRepository extends JpaRepository<Maquinas, UUID> {
    Optional<Maquinas> findByNome(String nome);

    @Transactional
    long deleteByNome(String nome);

    boolean existsByNome(String nome);
}