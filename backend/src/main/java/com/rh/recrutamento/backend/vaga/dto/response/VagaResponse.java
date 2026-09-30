package com.rh.recrutamento.backend.vaga.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Saída de vaga. */
public record VagaResponse(
    Long id,
    Long rhId,
    String titulo,
    String descricao,
    String requisitos,
    String local,
    String modalidade,
    String tipoContrato,
    String status,
    LocalDate prazo,
    LocalDateTime criadoEm,
    LocalDateTime atualizadoEm
) {}
