package com.rh.recrutamento.backend.documento.dto.response;

import java.time.LocalDateTime;

/** Saida de documento. Traz o candidato e a vaga para o RH separar os documentos por candidato. */
public record DocumentoResponse(
    Long id,
    Long candidaturaId,
    Long vagaId,
    String vagaTitulo,
    Long candidatoId,
    String candidatoNome,
    /** Nome para exibir (ex.: "Comprovante de residência"). */
    String tipo,
    /** Codigo da lista fechada (ex.: "comprovante_residencia"); nulo em envio antigo sem tipo correspondente. */
    String tipoCodigo,
    String formato,
    /** Revisao do RH: "pendente", "aprovado" ou "recusado". */
    String status,
    Long tamanhoBytes,
    LocalDateTime dataEnvio
) {}
