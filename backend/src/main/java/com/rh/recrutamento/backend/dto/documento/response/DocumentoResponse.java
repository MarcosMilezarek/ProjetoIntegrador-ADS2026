package com.rh.recrutamento.backend.dto.documento.response;

import java.time.LocalDateTime;

/** Saida de documento. Traz o candidato e a vaga para o RH separar os documentos por candidato. */
public record DocumentoResponse(
    Long id,
    Long candidaturaId,
    Long vagaId,
    String vagaTitulo,
    Long candidatoId,
    String candidatoNome,
    String tipo,
    String formato,
    Long tamanhoBytes,
    LocalDateTime dataEnvio
) {}
