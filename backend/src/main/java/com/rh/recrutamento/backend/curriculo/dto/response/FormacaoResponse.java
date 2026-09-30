package com.rh.recrutamento.backend.curriculo.dto.response;

import java.time.LocalDate;

public record FormacaoResponse(
    Long id,
    String curso,
    String instituicao,
    LocalDate dataInicio,
    LocalDate dataTermino
) {}
