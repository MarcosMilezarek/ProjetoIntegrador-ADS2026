package com.rh.recrutamento.backend.dto.curriculo.request;

/** Entrada para edicao de curriculo (PUT /curriculos/{id}). */
public record CurriculoUpdateRequest(
    String formacao,
    String experiencias,
    String competencias,
    String resumo
) {}
