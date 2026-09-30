package com.rh.recrutamento.backend.dto.candidatura.request;

import jakarta.validation.constraints.NotNull;

/** Entrada para se candidatar (POST /candidaturas). O candidato e quem esta logado. */
public record CandidaturaRequest(

    @NotNull(message = "A vaga é obrigatória.")
    Long vagaId
) {}
