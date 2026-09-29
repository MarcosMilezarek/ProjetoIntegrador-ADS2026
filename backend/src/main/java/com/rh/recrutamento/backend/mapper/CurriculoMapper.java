package com.rh.recrutamento.backend.mapper;

import com.rh.recrutamento.backend.dto.curriculo.response.CurriculoResponse;
import com.rh.recrutamento.backend.entity.Curriculo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;
import java.time.Period;

/** Conversao entre Curriculo e seu DTO de saida. */
@Mapper(componentModel = "spring")
public interface CurriculoMapper {

    @Mapping(target = "usuarioId", source = "usuario.id")
    @Mapping(target = "idade", expression = "java(calcularIdade(curriculo.getDataNascimento()))")
    CurriculoResponse toResponse(Curriculo curriculo);

    /** A idade nao e persistida: deriva da data de nascimento para nao envelhecer errado. */
    default Integer calcularIdade(LocalDate dataNascimento) {
        return dataNascimento == null ? null : Period.between(dataNascimento, LocalDate.now()).getYears();
    }
}
