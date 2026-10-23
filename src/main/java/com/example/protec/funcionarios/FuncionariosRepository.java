package com.example.protec.funcionarios;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.transaction.Transactional;


public interface FuncionariosRepository extends JpaRepository<Funcionarios, UUID>{
    Optional<Funcionarios> findByNome(String nome);

    @Transactional
    long deleteByNome(String nome);

    boolean existsByNome(String nome);
} 
