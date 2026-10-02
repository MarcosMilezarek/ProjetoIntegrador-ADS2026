package com.rh.recrutamento.backend.notificacao.dto.response;

import java.time.LocalDateTime;

/** Saida de notificacao, igual na listagem e no evento em tempo real. */
public record NotificacaoResponse(
    Long id,
    /** "candidatura" ou "nova_vaga". */
    String tipo,
    /** Id da candidatura (tipo candidatura) ou da vaga (tipo nova_vaga). Nulo nas notificacoes antigas. */
    Long referenciaId,
    String titulo,
    String mensagem,
    boolean lida,
    LocalDateTime criadoEm
) {}
