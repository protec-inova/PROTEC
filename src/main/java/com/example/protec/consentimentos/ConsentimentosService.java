package com.example.protec.consentimentos;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.protec.funcionarios.FuncionariosRepository;
import com.example.protec.maquinas.MaquinasRepository;

@Service
public class ConsentimentosService {

    // Tempo em que um aceite do termo continua valendo.
    private static final Duration VALIDADE = Duration.ofHours(1);

    private static final int CA_MAX_LENGTH = 20;

    private static final int PAGE_SIZE_MAX = 100;

    private final ConsentimentosRepository consentimentosRepository;
    private final FuncionariosRepository funcionariosRepository;
    private final MaquinasRepository maquinasRepository;

    public ConsentimentosService(
            ConsentimentosRepository consentimentosRepository,
            FuncionariosRepository funcionariosRepository,
            MaquinasRepository maquinasRepository) {
        this.consentimentosRepository = consentimentosRepository;
        this.funcionariosRepository = funcionariosRepository;
        this.maquinasRepository = maquinasRepository;
    }

    // ── Getters ─────────────────────────────────────────────────────────────────

    public ConsentimentosResponse getById(Integer id) {
        Consentimentos consentimento = consentimentosRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("consentimento not found"));

        return toResponse(consentimento);
    }

    // Último aceite de um funcionário (o campo "valido" diz se ainda vale).
    public ConsentimentosResponse getUltimoByFuncionario(UUID funcionarioId) {
        Consentimentos consentimento = consentimentosRepository
                .findFirstByFuncionarioIdOrderByDataDesc(funcionarioId)
                .orElseThrow(() -> new IllegalArgumentException("consentimento not found"));

        return toResponse(consentimento);
    }

    // Último aceite registrado para uma máquina (o campo "valido" diz se ainda vale).
    public ConsentimentosResponse getUltimoByMaquina(UUID maquinaId) {
        Consentimentos consentimento = consentimentosRepository
                .findFirstByMaquinaIdOrderByDataDesc(maquinaId)
                .orElseThrow(() -> new IllegalArgumentException("consentimento not found"));

        return toResponse(consentimento);
    }

    // Histórico de aceites de um funcionário (mais recente primeiro), paginado.
    public List<ConsentimentosResponse> getByFuncionario(UUID funcionarioId, int page, int size) {
        if (!funcionariosRepository.existsById(funcionarioId)) {
            throw new IllegalArgumentException("funcionario not found");
        }

        return consentimentosRepository.findByFuncionarioIdOrderByDataDesc(funcionarioId, pageable(page, size))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Histórico de aceites de uma máquina (mais recente primeiro), paginado.
    public List<ConsentimentosResponse> getByMaquina(UUID maquinaId, int page, int size) {
        if (!maquinasRepository.existsById(maquinaId)) {
            throw new IllegalArgumentException("maquina not found");
        }

        return consentimentosRepository.findByMaquinaIdOrderByDataDesc(maquinaId, pageable(page, size))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ConsentimentosResponse> getAll() {
        return consentimentosRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Create ─────────────────────────────────────────────────────────────────

    public ConsentimentosResponse create(UUID funcionarioId, UUID maquinaId, String oculos, String bota) {
        if (funcionarioId == null || !funcionariosRepository.existsById(funcionarioId)) {
            throw new IllegalArgumentException("funcionario not found");
        }

        if (maquinaId == null || !maquinasRepository.existsById(maquinaId)) {
            throw new IllegalArgumentException("maquina not found");
        }

        if (oculos != null && oculos.length() > CA_MAX_LENGTH) {
            throw new IllegalArgumentException("oculos too long");
        }

        if (bota != null && bota.length() > CA_MAX_LENGTH) {
            throw new IllegalArgumentException("bota too long");
        }

        Consentimentos consentimento = new Consentimentos();
        consentimento.setFuncionarioId(funcionarioId);
        consentimento.setMaquinaId(maquinaId);
        consentimento.setOculos(blankToNull(oculos));
        consentimento.setBota(blankToNull(bota));
        consentimentosRepository.save(consentimento);

        return toResponse(consentimento);
    }

    // page começa em 0; valores fora do limite são ajustados (size entre 1 e 100).
    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), PAGE_SIZE_MAX));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private ConsentimentosResponse toResponse(Consentimentos consentimento) {
        Instant expiraEm = consentimento.getData().plus(VALIDADE);
        long segundosRestantes = Math.max(0,
                Duration.between(Instant.now(), expiraEm).getSeconds());

        return new ConsentimentosResponse(
                consentimento.getId(),
                consentimento.getFuncionarioId(),
                consentimento.getMaquinaId(),
                consentimento.getOculos(),
                consentimento.getBota(),
                consentimento.getData(),
                expiraEm,
                segundosRestantes > 0,
                segundosRestantes);
    }
}