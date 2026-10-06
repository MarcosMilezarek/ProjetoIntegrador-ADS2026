package com.rh.recrutamento.backend.curriculo.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Um item de experiencia profissional. Se trabalhoAtual for true, dataDemissao e descartada. */
public record ExperienciaRequest(

    @NotBlank(message = "O cargo e obrigatorio.")
    @Size(max = 150, message = "O cargo deve ter no maximo 150 caracteres.")
    String cargo,

    @NotBlank(message = "A empresa e obrigatoria.")
    @Size(max = 150, message = "A empresa deve ter no maximo 150 caracteres.")
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
