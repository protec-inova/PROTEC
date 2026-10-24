package com.example.protec.maquinas;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/maquinas")
public class MaquinasController {

    private final MaquinasService maquinasService;
    private final MaquinaGateway maquinaGateway;

    public MaquinasController(
            MaquinasService maquinasService,
            MaquinaGateway maquinaGateway) {

        this.maquinasService = maquinasService;
        this.maquinaGateway = maquinaGateway;
    }

    // ── Getters ─────────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<MaquinasResponse>> listAll() {
        return ResponseEntity.ok(maquinasService.getAll());
    }

    @GetMapping("/available")
    public ResponseEntity<List<MaquinasResponse>> available() {
        return ResponseEntity.ok(maquinaGateway.findAvailable());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaquinasResponse> getById(
            @PathVariable UUID id) {
        try {
            return ResponseEntity.ok(maquinasService.getById(id));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/nome/{nome}")
    public ResponseEntity<MaquinasResponse> getByNome(
            @PathVariable String nome) {

        try {
            return ResponseEntity.ok(maquinasService.getByNome(nome));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // ── Create ──────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<MaquinasResponse> create(
            @RequestParam String nome,
            @RequestParam String riscos) {

        try {
            return new ResponseEntity<>(maquinasService.create(nome, riscos), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    // ── Update ──────────────────────────────────────────────────────────────────

    @PatchMapping("/nome/{nome}/nome")
    public ResponseEntity<MaquinasResponse> updateNome(
            @PathVariable String nome,
            @RequestParam String newNome) {

        try {
            return ResponseEntity.ok(maquinasService.updateNome(nome, newNome));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PatchMapping("/{id}/nome")
    public ResponseEntity<MaquinasResponse> updateNome(
            @PathVariable UUID id,
            @RequestParam String nome) {

        try {
            return ResponseEntity.ok(maquinasService.updateNome(id, nome));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PatchMapping("/{id}/riscos")
    public ResponseEntity<MaquinasResponse> updateRiscos(
            @PathVariable UUID id,
            @RequestParam String riscos) {

        try {
            return ResponseEntity.ok(maquinasService.updateRiscos(id, riscos));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PatchMapping("/nome/{nome}/riscos")
    public ResponseEntity<MaquinasResponse> updateRiscos(
            @PathVariable String nome,
            @RequestParam String riscos) {

        try {
            return ResponseEntity.ok(maquinasService.updateRiscos(nome, riscos));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // ── Delete ─────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id) {

        try {
            maquinasService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/nome/{nome}")
    public ResponseEntity<Void> delete(
            @PathVariable String nome) {

        try {
            maquinasService.delete(nome);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}