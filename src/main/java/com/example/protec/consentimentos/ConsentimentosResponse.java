package com.example.protec.consentimentos;

import java.time.Instant;
import java.util.UUID;

public record ConsentimentosResponse(
    Integer id,
    UUID funcionarioId,
    UUID maquinaId,
    String oculos,
    String bota,
    Instant data,
    Instant expiraEm,
    boolean valido,
    long segundosRestantes
) {}