package com.rh.recrutamento.backend.mapper;

import com.rh.recrutamento.backend.dto.vaga.response.VagaResponse;
import com.rh.recrutamento.backend.entity.Vaga;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Conversão entre {@link Vaga} e seu DTO de saída. */
@Mapper(componentModel = "spring")
public interface VagaMapper {

    @Mapping(target = "rhId", source = "rh.id")
    VagaResponse toResponse(Vaga vaga);
}
