package com.example.protec.funcionarios;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/funcionarios")
public class FuncionariosController {

    private final FuncionariosService funcionariosService;

    public FuncionariosController(
        FuncionariosService funcionariosService) {

        this.funcionariosService = funcionariosService;
    }

    // ── Getters ─────────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<FuncionariosResponse>> listAll() {
        return ResponseEntity.ok(funcionariosService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FuncionariosResponse> getById(
            @PathVariable UUID id) {
        try {
            return ResponseEntity.ok(funcionariosService.getById(id));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/nome/{nome}")
    public ResponseEntity<FuncionariosResponse> getByNome(
            @PathVariable String nome) {

        try {
            return ResponseEntity.ok(funcionariosService.getByNome(nome));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // ── Create ──────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<FuncionariosResponse> create(
            @RequestParam String nome) {

        try {
            return new ResponseEntity<>(funcionariosService.create(nome), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    // ── Update ──────────────────────────────────────────────────────────────────

    @PatchMapping("/nome/{nome}/nome")
    public ResponseEntity<FuncionariosResponse> updateNome(
            @PathVariable String nome,
            @RequestParam String newNome) {

        try {
            return ResponseEntity.ok(funcionariosService.updateNome(nome, newNome));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PatchMapping("/{id}/nome")
    public ResponseEntity<FuncionariosResponse> updateNome(
            @PathVariable UUID id,
            @RequestParam String nome) {

        try {
            return ResponseEntity.ok(funcionariosService.updateNome(id, nome));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // ── Delete ─────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id) {

        try {
            funcionariosService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/nome/{nome}")
    public ResponseEntity<Void> delete(
            @PathVariable String nome) {

        try {
            funcionariosService.delete(nome);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
