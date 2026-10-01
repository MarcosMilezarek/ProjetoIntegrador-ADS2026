package com.rh.recrutamento.backend.documento.dto.response;

import com.rh.recrutamento.backend.documento.entity.Documento;

/** Item da lista fechada de tipos de documento. O frontend envia o codigo no upload. */
public record TipoDocumentoResponse(
    String codigo,
    String nome,
    boolean obrigatorio,
    /** Quando o documento passa a ser exigido; nulo nos obrigatorios. */
    String condicao
) {
    public static TipoDocumentoResponse de(Documento.Tipo tipo) {
        return new TipoDocumentoResponse(tipo.name(), tipo.getNome(), tipo.isObrigatorio(), tipo.getCondicao());
    }
}
