package com.rh.recrutamento.backend.dto.vaga.request;

import com.rh.recrutamento.backend.entity.Vaga;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Entrada para cadastro de vaga (POST /vagas). */
public record VagaRequest(

    @NotNull(message = "O RH responsável é obrigatório.")
    Long rhId,

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

    /** Opcional: quando ausente a vaga é criada como rascunho. */
    Vaga.Status status,

    LocalDate prazo
) {}
