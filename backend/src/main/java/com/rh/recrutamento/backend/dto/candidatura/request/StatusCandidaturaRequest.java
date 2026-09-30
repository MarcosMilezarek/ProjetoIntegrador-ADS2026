package com.rh.recrutamento.backend.dto.candidatura.request;

import com.rh.recrutamento.backend.entity.Candidatura;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Entrada para mudar a etapa do candidato (PUT /candidaturas/{id}/status). */
public record StatusCandidaturaRequest(

    @NotNull(message = "O status é obrigatório.")
    Candidatura.Status status,

    @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres.")
    String observacao
) {}
