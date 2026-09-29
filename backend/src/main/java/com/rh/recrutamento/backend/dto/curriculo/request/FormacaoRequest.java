package com.rh.recrutamento.backend.dto.curriculo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Um item de formacao academica. */
public record FormacaoRequest(

    @NotBlank(message = "O curso e obrigatorio.")
    String curso,

    @NotBlank(message = "A instituicao e obrigatoria.")
    String instituicao,

    @NotNull(message = "A data de inicio e obrigatoria.")
    LocalDate dataInicio,

    /** Nula quando o curso esta em andamento. */
    LocalDate dataTermino
) {}
