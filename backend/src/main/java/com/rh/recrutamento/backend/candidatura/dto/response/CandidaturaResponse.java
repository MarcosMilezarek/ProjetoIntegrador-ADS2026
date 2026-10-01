package com.rh.recrutamento.backend.candidatura.dto.response;

import java.time.Instant;
import java.time.LocalDateTime;

/** Saida de candidatura: serve ao candidato (minhas candidaturas) e ao RH (inscritos por vaga). */
public record CandidaturaResponse(
    Long id,
    Long vagaId,
    String vagaTitulo,
    String vagaStatus,
    Long candidatoId,
    String candidatoNome,
    String candidatoEmail,
    String status,
    /** Entrevista marcada, em UTC (ex.: 2026-10-15T17:30:00Z). Nula enquanto nao houver. */
    Instant entrevistaEm,
    LocalDateTime dataCandidatura
) {}
