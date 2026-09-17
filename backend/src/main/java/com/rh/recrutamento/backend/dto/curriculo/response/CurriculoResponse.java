package com.rh.recrutamento.backend.dto.curriculo.response;

import java.time.LocalDateTime;

/** Saida de curriculo. */
public record CurriculoResponse(
    Long id,
    Long usuarioId,
    String formacao,
    String experiencias,
    String competencias,
    String resumo,
    LocalDateTime atualizadoEm
) {}
