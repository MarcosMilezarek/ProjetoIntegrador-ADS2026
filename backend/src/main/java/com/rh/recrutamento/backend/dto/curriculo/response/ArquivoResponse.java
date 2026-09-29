package com.rh.recrutamento.backend.dto.curriculo.response;

import java.time.LocalDateTime;

/** Metadados do PDF anexado. O download e feito em GET /curriculos/{id}/arquivo. */
public record ArquivoResponse(
    Long id,
    String nomeOriginal,
    String contentType,
    Long tamanhoBytes,
    LocalDateTime enviadoEm
) {}
