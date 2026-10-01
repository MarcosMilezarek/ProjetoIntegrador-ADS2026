package com.rh.recrutamento.backend.documento.dto.response;

import java.util.List;

/**
 * Quadro de documentos de uma candidatura: o que foi enviado, o que falta e a contagem de
 * obrigatorios enviados sobre o total exigido. O candidato e o RH veem o mesmo quadro.
 */
public record DocumentosDaCandidaturaResponse(
    Long candidaturaId,
    Long candidatoId,
    String candidatoNome,
    Long vagaId,
    String vagaTitulo,
    long enviadosExigidos,
    long totalExigidos,
    List<DocumentoResponse> enviados,
    List<TipoDocumentoResponse> pendentes
) {}
