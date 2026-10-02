package com.rh.recrutamento.backend.funcionario.dto.response;

import com.rh.recrutamento.backend.documento.dto.response.DocumentoResponse;

import java.util.List;

/** Perfil do funcionario: os dados da contratacao e os documentos da candidatura (download em GET /documentos/{id}/arquivo). */
public record FuncionarioPerfilResponse(
    FuncionarioResponse funcionario,
    List<DocumentoResponse> documentos
) {}
