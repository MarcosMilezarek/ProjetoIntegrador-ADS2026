package com.rh.recrutamento.backend.curriculo.dto.response;

import com.rh.recrutamento.backend.curriculo.entity.Curriculo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Saida de curriculo. A idade e derivada de dataNascimento, nao persistida. */
public record CurriculoResponse(
    Long id,
    Long usuarioId,
    LocalDate dataNascimento,
    Integer idade,
    Curriculo.Sexo sexo,
    String cidade,
    String uf,
    String numeroContato,
    String perfilLinkedin,
    String competencias,
    String certificacoes,
    String resumo,
    List<FormacaoResponse> formacoes,
    List<ExperienciaResponse> experiencias,
    ArquivoResponse arquivo,
    LocalDateTime atualizadoEm
) {}
