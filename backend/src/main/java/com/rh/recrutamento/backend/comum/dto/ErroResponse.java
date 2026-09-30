package com.rh.recrutamento.backend.comum.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/** Corpo padrão de erro devolvido pelo GlobalExceptionHandler. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(
    LocalDateTime timestamp,
    int status,
    String erro,
    String mensagem,
    Map<String, String> campos
) {
    public static ErroResponse de(int status, String erro, String mensagem) {
        return new ErroResponse(LocalDateTime.now(), status, erro, mensagem, null);
    }

    public static ErroResponse de(int status, String erro, String mensagem, Map<String, String> campos) {
        return new ErroResponse(LocalDateTime.now(), status, erro, mensagem, campos);
    }
}
