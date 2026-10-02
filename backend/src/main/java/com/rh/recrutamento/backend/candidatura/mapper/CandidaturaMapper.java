package com.rh.recrutamento.backend.candidatura.mapper;

import com.rh.recrutamento.backend.candidatura.dto.response.CandidaturaResponse;
import com.rh.recrutamento.backend.candidatura.entity.Candidatura;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Conversao entre Candidatura e seu DTO de saida. */
@Mapper(componentModel = "spring")
public interface CandidaturaMapper {

    @Mapping(target = "vagaId", source = "vaga.id")
    @Mapping(target = "vagaTitulo", source = "vaga.titulo")
    @Mapping(target = "vagaStatus", source = "vaga.status")
    @Mapping(target = "candidatoId", source = "candidato.id")
    @Mapping(target = "candidatoNome", source = "candidato.nome")
    @Mapping(target = "candidatoEmail", source = "candidato.email")
    @Mapping(target = "entrevistaEm", expression = "java(emUtc(candidatura.getEntrevistaEm()))")
    @Mapping(target = "presencaConfirmadaEm", expression = "java(emUtc(candidatura.getPresencaConfirmadaEm()))")
    CandidaturaResponse toResponse(Candidatura candidatura);

    /** A coluna guarda a hora em UTC sem fuso; aqui ela volta a ser um instante. */
    default Instant emUtc(LocalDateTime utc) {
        return utc == null ? null : utc.toInstant(ZoneOffset.UTC);
    }
}
