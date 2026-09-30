package com.rh.recrutamento.backend.candidatura.dto.request;

import jakarta.validation.constraints.NotNull;

/** Entrada para se candidatar (POST /candidaturas). O candidato e quem esta logado. */
public record CandidaturaRequest(

    @NotNull(message = "A vaga é obrigatória.")
    Long vagaId
) {}
