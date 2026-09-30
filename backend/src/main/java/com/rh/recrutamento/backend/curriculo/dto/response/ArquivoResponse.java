package com.rh.recrutamento.backend.curriculo.dto.response;

import java.time.LocalDateTime;

/** Metadados do PDF anexado. O download e feito em GET /curriculos/{id}/arquivo. */
public record ArquivoResponse(
    Long id,
    String nomeOriginal,
    String contentType,
    Long tamanhoBytes,
    LocalDateTime enviadoEm
) {}
