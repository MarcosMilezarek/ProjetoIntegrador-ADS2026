package com.rh.recrutamento.backend.candidatura.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Entrada para marcar a entrevista (PUT /candidaturas/{id}/entrevista). A data e hora vem em
 * ISO 8601 com fuso, ex.: 2026-10-15T14:30:00-03:00 ou 2026-10-15T17:30:00Z, e e gravada em UTC.
 */
public record EntrevistaRequest(

    @NotNull(message = "Informe a data e a hora da entrevista.")
    @Future(message = "A entrevista deve ser marcada para uma data e hora futuras.")
    Instant dataHora
) {}
