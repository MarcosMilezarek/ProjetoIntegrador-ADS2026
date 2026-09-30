package com.rh.recrutamento.backend.mapper;

import com.rh.recrutamento.backend.dto.candidatura.response.CandidaturaResponse;
import com.rh.recrutamento.backend.entity.Candidatura;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Conversao entre Candidatura e seu DTO de saida. */
@Mapper(componentModel = "spring")
public interface CandidaturaMapper {

    @Mapping(target = "vagaId", source = "vaga.id")
    @Mapping(target = "vagaTitulo", source = "vaga.titulo")
    @Mapping(target = "vagaStatus", source = "vaga.status")
    @Mapping(target = "candidatoId", source = "candidato.id")
    @Mapping(target = "candidatoNome", source = "candidato.nome")
    @Mapping(target = "candidatoEmail", source = "candidato.email")
    CandidaturaResponse toResponse(Candidatura candidatura);
}
