package com.rh.recrutamento.backend.notificacao.mapper;

import com.rh.recrutamento.backend.notificacao.dto.response.NotificacaoResponse;
import com.rh.recrutamento.backend.notificacao.entity.Notificacao;
import org.mapstruct.Mapper;

/** Conversao entre Notificacao e seu DTO de saida. */
@Mapper(componentModel = "spring")
public interface NotificacaoMapper {

    NotificacaoResponse toResponse(Notificacao notificacao);
}
