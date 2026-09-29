package com.rh.recrutamento.backend.dto.curriculo.request;

import jakarta.validation.constraints.AssertTrue;
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
) {
    /** Espelha a constraint chk_experiencia_periodo do banco, para o erro sair como 400 e nao 500. */
    @AssertTrue(message = "A data de demissao nao pode ser anterior a data de contratacao.")
    public boolean isPeriodoValido() {
        return dataDemissao == null || dataContratacao == null || !dataDemissao.isBefore(dataContratacao);
    }
}
