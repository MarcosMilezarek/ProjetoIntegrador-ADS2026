package com.rh.recrutamento.backend.dto.curriculo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Um item de experiencia profissional. Se trabalhoAtual for true, dataDemissao e descartada. */
public record ExperienciaRequest(

    @NotBlank(message = "O cargo e obrigatorio.")
    String cargo,

    @NotBlank(message = "A empresa e obrigatoria.")
    String empresa,

    @NotNull(message = "A data de contratacao e obrigatoria.")
    LocalDate dataContratacao,

    LocalDate dataDemissao,

    boolean trabalhoAtual,

    String descricaoAtividades
) {}
