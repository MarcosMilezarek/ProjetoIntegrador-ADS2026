package com.rh.recrutamento.backend.funcionario.dto.response;

import java.time.LocalDateTime;

/** Saida de funcionario: quem foi contratado, em qual vaga e quando. */
public record FuncionarioResponse(
    Long id,
    Long candidaturaId,
    Long candidatoId,
    String candidatoNome,
    String candidatoEmail,
    Long vagaId,
    String vagaTitulo,
    /** "ativo" ou "inativo". */
    String status,
    LocalDateTime dataContratacao
) {}
