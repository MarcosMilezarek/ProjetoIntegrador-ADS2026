package com.rh.recrutamento.backend.funcionario.mapper;

import com.rh.recrutamento.backend.funcionario.dto.response.FuncionarioResponse;
import com.rh.recrutamento.backend.funcionario.entity.Funcionario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Conversao entre Funcionario e seu DTO de saida. */
@Mapper(componentModel = "spring")
public interface FuncionarioMapper {

    @Mapping(target = "candidaturaId", source = "candidatura.id")
    @Mapping(target = "candidatoId", source = "candidatura.candidato.id")
    @Mapping(target = "candidatoNome", source = "candidatura.candidato.nome")
    @Mapping(target = "candidatoEmail", source = "candidatura.candidato.email")
    @Mapping(target = "vagaId", source = "candidatura.vaga.id")
    @Mapping(target = "vagaTitulo", source = "candidatura.vaga.titulo")
    FuncionarioResponse toResponse(Funcionario funcionario);
}
