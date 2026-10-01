package com.rh.recrutamento.backend.notificacao.dto.response;

import java.time.LocalDateTime;

/** Saida de notificacao, igual na listagem e no evento em tempo real. */
public record NotificacaoResponse(
    Long id,
    String titulo,
    String mensagem,
    boolean lida,
    LocalDateTime criadoEm
) {}
