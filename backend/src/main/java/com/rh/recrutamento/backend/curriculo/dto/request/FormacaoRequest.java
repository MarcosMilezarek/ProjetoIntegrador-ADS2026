package com.rh.recrutamento.backend.curriculo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Um item de formacao academica. */
public record FormacaoRequest(

    @NotBlank(message = "O curso e obrigatorio.")
    @Size(max = 150, message = "O curso deve ter no maximo 150 caracteres.")
    String curso,

    @NotBlank(message = "A instituicao e obrigatoria.")
    @Size(max = 150, message = "A instituicao deve ter no maximo 150 caracteres.")
    String instituicao,

    @NotNull(message = "A data de inicio e obrigatoria.")
    LocalDate dataInicio,

    /** Nula quando o curso esta em andamento. */
    LocalDate dataTermino
) {}
