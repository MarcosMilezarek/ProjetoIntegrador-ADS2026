package com.rh.recrutamento.backend.dto.curriculo.response;

import java.time.LocalDate;

public record FormacaoResponse(
    Long id,
    String curso,
    String instituicao,
    LocalDate dataInicio,
    LocalDate dataTermino
) {}
