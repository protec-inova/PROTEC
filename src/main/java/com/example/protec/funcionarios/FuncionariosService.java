package com.example.protec.funcionarios;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class FuncionariosService {
    private final FuncionariosRepository funcionariosRepository;

    public FuncionariosService(FuncionariosRepository funcionariosRepository) {
        this.funcionariosRepository = funcionariosRepository;
    }

    // ── Getters ─────────────────────────────────────────────────────────────────

    public FuncionariosResponse getById(UUID id) {
        Funcionarios funcionarios = funcionariosRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("funcionario not found"));

        return toResponse(funcionarios);
    }

    public FuncionariosResponse getByNome(String nome) {
        Funcionarios funcionario = funcionariosRepository.findByNome(nome)
                .orElseThrow(() -> new IllegalArgumentException("funcionario not found"));

        return toResponse(funcionario);
    }

    public List<FuncionariosResponse> getAll() {
        return funcionariosRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Create ─────────────────────────────────────────────────────────────────

    public FuncionariosResponse create(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome cannot be blank");
        }

        if (funcionariosRepository.existsByNome(nome)) {
            throw new IllegalArgumentException("nome already taken");
        }

        Funcionarios funcionario = new Funcionarios();
        funcionario.setNome(nome);
        funcionariosRepository.save(funcionario);

        return toResponse(funcionario);
    }

    // ── Updates ─────────────────────────────────────────────────────────────────

    public FuncionariosResponse updateNome(UUID id, String newNome) {
        if (newNome == null || newNome.isBlank()) {
            throw new IllegalArgumentException("nome cannot be blank");
        }

        if (funcionariosRepository.existsByNome(newNome)) {
            throw new IllegalArgumentException("nome already taken");
        }

        Funcionarios funcionario = funcionariosRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("funcionario not found"));

        funcionario.setNome(newNome);
        funcionariosRepository.save(funcionario);

        return toResponse(funcionario);
    }

    public FuncionariosResponse updateNome(String nome, String newNome) {
        if (newNome == null || newNome.isBlank()) {
            throw new IllegalArgumentException("nome cannot be blank");
        }

        if (funcionariosRepository.existsByNome(newNome)) {
            throw new IllegalArgumentException("nome already taken");
        }

        Funcionarios funcionario = funcionariosRepository.findByNome(nome)
                .orElseThrow(() -> new IllegalArgumentException("funcionario not found"));

        funcionario.setNome(newNome);
        funcionariosRepository.save(funcionario);

        return toResponse(funcionario);
    }

    // ── Delete ─────────────────────────────────────────────────────────────────

    public void delete(UUID id) {
        if (!funcionariosRepository.existsById(id)) {
            throw new IllegalArgumentException("funcionario not found");
        }

        funcionariosRepository.deleteById(id);
    }

    public void delete(String nome) {
        if (!funcionariosRepository.existsByNome(nome)) {
            throw new IllegalArgumentException("funcionario not found");
        }

        funcionariosRepository.deleteByNome(nome);
    }

    private FuncionariosResponse toResponse(Funcionarios funcionario) {
        return new FuncionariosResponse(
                funcionario.getId(),
                funcionario.getNome());
    }
}
