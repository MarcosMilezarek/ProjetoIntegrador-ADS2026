package com.rh.recrutamento.backend.vaga.mapper;

import com.rh.recrutamento.backend.vaga.dto.response.VagaResponse;
import com.rh.recrutamento.backend.vaga.entity.Vaga;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Conversão entre {@link Vaga} e seu DTO de saída. */
@Mapper(componentModel = "spring")
public interface VagaMapper {

    @Mapping(target = "rhId", source = "rh.id")
    VagaResponse toResponse(Vaga vaga);
}
