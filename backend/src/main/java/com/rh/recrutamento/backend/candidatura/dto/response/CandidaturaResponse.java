package com.rh.recrutamento.backend.candidatura.dto.response;

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
    LocalDateTime dataCandidatura
) {}
