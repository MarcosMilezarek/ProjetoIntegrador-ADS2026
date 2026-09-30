package com.rh.recrutamento.backend.mapper;

import com.rh.recrutamento.backend.dto.documento.response.DocumentoResponse;
import com.rh.recrutamento.backend.entity.Documento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Conversao entre Documento e seu DTO de saida. */
@Mapper(componentModel = "spring")
public interface DocumentoMapper {

    @Mapping(target = "candidaturaId", source = "candidatura.id")
    @Mapping(target = "vagaId", source = "candidatura.vaga.id")
    @Mapping(target = "vagaTitulo", source = "candidatura.vaga.titulo")
    @Mapping(target = "candidatoId", source = "candidatura.candidato.id")
    @Mapping(target = "candidatoNome", source = "candidatura.candidato.nome")
    DocumentoResponse toResponse(Documento documento);
}
