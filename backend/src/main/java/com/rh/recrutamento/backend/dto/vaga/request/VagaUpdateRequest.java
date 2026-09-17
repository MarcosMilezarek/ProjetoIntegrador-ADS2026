package com.rh.recrutamento.backend.dto.vaga.request;

import com.rh.recrutamento.backend.entity.Vaga;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Entrada para edição ou encerramento de vaga (PUT /vagas/{id}). Encerrar = enviar status=encerrada. */
public record VagaUpdateRequest(

    @NotBlank(message = "O título é obrigatório.")
    @Size(max = 150, message = "O título deve ter no máximo 150 caracteres.")
    String titulo,

    @NotBlank(message = "A descrição é obrigatória.")
    String descricao,

    String requisitos,

    @Size(max = 150, message = "O local deve ter no máximo 150 caracteres.")
    String local,

    @NotNull(message = "A modalidade é obrigatória.")
    Vaga.Modalidade modalidade,

    @NotNull(message = "O tipo de contratação é obrigatório.")
    Vaga.TipoContrato tipoContrato,

    @NotNull(message = "O status é obrigatório.")
    Vaga.Status status,

    LocalDate prazo
) {}
