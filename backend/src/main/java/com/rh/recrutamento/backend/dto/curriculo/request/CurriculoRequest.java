package com.rh.recrutamento.backend.dto.curriculo.request;

import com.rh.recrutamento.backend.entity.Curriculo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Entrada para cadastro de curriculo (POST /curriculos). */
public record CurriculoRequest(

    @NotNull(message = "O usuario candidato e obrigatorio.")
    Long usuarioId,

    @Past(message = "A data de nascimento deve ser no passado.")
    LocalDate dataNascimento,

    Curriculo.Sexo sexo,

    @Size(max = 100, message = "A cidade deve ter no maximo 100 caracteres.")
    String cidade,

    @Size(min = 2, max = 2, message = "A UF deve ter 2 caracteres.")
    String uf,

    @Size(max = 20, message = "O numero de contato deve ter no maximo 20 caracteres.")
    String numeroContato,

    @Size(max = 255, message = "O perfil do LinkedIn deve ter no maximo 255 caracteres.")
    String perfilLinkedin,

    String competencias,

    /** Certificacoes fora da formacao academica; opcional. */
    String certificacoes,

    String resumo,

    @Valid
    List<FormacaoRequest> formacoes,

    @Valid
    List<ExperienciaRequest> experiencias
) {}
