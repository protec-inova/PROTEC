package com.example.protec.maquinas;

import java.util.UUID;

public record MaquinasResponse(
    UUID id,
    String nome,
    String riscos,
    boolean ativade
) {} 