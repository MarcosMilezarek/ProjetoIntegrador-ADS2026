package com.rh.recrutamento.backend.documento.mapper;

import com.rh.recrutamento.backend.documento.dto.response.DocumentoResponse;
import com.rh.recrutamento.backend.documento.entity.Documento;
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
    @Mapping(target = "tipo", expression = "java(documento.nomeDoTipo())")
    @Mapping(target = "tipoCodigo", source = "tipo")
    DocumentoResponse toResponse(Documento documento);
}
