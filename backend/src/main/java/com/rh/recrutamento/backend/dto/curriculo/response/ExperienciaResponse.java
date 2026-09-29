package com.rh.recrutamento.backend.dto.curriculo.response;

import java.time.LocalDate;

public record ExperienciaResponse(
    Long id,
    String cargo,
    String empresa,
    LocalDate dataContratacao,
    LocalDate dataDemissao,
    boolean trabalhoAtual,
    String descricaoAtividades
) {}
