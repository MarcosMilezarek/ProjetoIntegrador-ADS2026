package com.rh.recrutamento.backend.mapper;

import com.rh.recrutamento.backend.dto.curriculo.response.CurriculoResponse;
import com.rh.recrutamento.backend.entity.Curriculo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Conversao entre Curriculo e seu DTO de saida. */
@Mapper(componentModel = "spring")
public interface CurriculoMapper {

    @Mapping(target = "usuarioId", source = "candidato.usuarioId")
    CurriculoResponse toResponse(Curriculo curriculo);
}
