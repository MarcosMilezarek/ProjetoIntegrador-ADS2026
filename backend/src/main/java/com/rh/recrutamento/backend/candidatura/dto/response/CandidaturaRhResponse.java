package com.rh.recrutamento.backend.candidatura.dto.response;

import com.rh.recrutamento.backend.analise.dto.response.AnaliseResponse;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Candidatura vista pelo RH: os mesmos campos de {@link CandidaturaResponse} mais a triagem por IA.
 * E um tipo a parte de proposito: o candidato so recebe CandidaturaResponse, que nao tem a analise.
 */
public record CandidaturaRhResponse(
    Long id,
    Long vagaId,
    String vagaTitulo,
    String vagaStatus,
    Long candidatoId,
    String candidatoNome,
    String candidatoEmail,
    String status,
    Instant entrevistaEm,
    String presenca,
    Instant presencaConfirmadaEm,
    LocalDateTime dataCandidatura,
    /** Nula nas candidaturas anteriores a triagem por IA. */
    AnaliseResponse analise
) {

    public static CandidaturaRhResponse de(CandidaturaResponse c, AnaliseResponse analise) {
        return new CandidaturaRhResponse(c.id(), c.vagaId(), c.vagaTitulo(), c.vagaStatus(), c.candidatoId(),
            c.candidatoNome(), c.candidatoEmail(), c.status(), c.entrevistaEm(), c.presenca(),
            c.presencaConfirmadaEm(), c.dataCandidatura(), analise);
    }
}
