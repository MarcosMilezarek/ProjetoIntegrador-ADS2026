package com.rh.recrutamento.backend.dto.curriculo.request;

import jakarta.validation.constraints.NotNull;

/** Entrada para cadastro de curriculo (POST /curriculos). */
public record CurriculoRequest(

    @NotNull(message = "O usuario candidato e obrigatorio.")
    Long usuarioId,

    String formacao,
    String experiencias,
    String competencias,
    String resumo
) {}
