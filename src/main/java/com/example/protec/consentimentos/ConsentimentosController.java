package com.example.protec.consentimentos;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/consentimentos")
public class ConsentimentosController {

    private final ConsentimentosService consentimentosService;

    public ConsentimentosController(
        ConsentimentosService consentimentosService) {

        this.consentimentosService = consentimentosService;
    }

    // ── Getters ─────────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<ConsentimentosResponse>> listAll() {
        return ResponseEntity.ok(consentimentosService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConsentimentosResponse> getById(
            @PathVariable Integer id) {
        try {
            return ResponseEntity.ok(consentimentosService.getById(id));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/funcionario/{funcionarioId}")
    public ResponseEntity<List<ConsentimentosResponse>> getByFuncionario(
            @PathVariable UUID funcionarioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            return ResponseEntity.ok(consentimentosService.getByFuncionario(funcionarioId, page, size));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/maquina/{maquinaId}")
    public ResponseEntity<List<ConsentimentosResponse>> getByMaquina(
            @PathVariable UUID maquinaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            return ResponseEntity.ok(consentimentosService.getByMaquina(maquinaId, page, size));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // Informe funcionarioId OU maquinaId (exatamente um dos dois).
    @GetMapping("/ultimo")
    public ResponseEntity<ConsentimentosResponse> getUltimo(
            @RequestParam(required = false) UUID funcionarioId,
            @RequestParam(required = false) UUID maquinaId) {

        if ((funcionarioId == null) == (maquinaId == null)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            return ResponseEntity.ok(funcionarioId != null
                    ? consentimentosService.getUltimoByFuncionario(funcionarioId)
                    : consentimentosService.getUltimoByMaquina(maquinaId));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // ── Create ──────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ConsentimentosResponse> create(
            @RequestParam UUID funcionarioId,
            @RequestParam UUID maquinaId,
            @RequestParam(required = false) String oculos,
            @RequestParam(required = false) String bota) {

        try {
            return new ResponseEntity<>(
                    consentimentosService.create(funcionarioId, maquinaId, oculos, bota),
                    HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}