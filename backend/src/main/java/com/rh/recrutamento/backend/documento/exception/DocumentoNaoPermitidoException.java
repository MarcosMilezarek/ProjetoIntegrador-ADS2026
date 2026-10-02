package com.rh.recrutamento.backend.documento.exception;

import com.rh.recrutamento.backend.comum.exception.NegocioException;

/** Envio de documento antes da aprovacao na vaga (RN03). Mapeada para 409 Conflict. */
public class DocumentoNaoPermitidoException extends NegocioException {

    public DocumentoNaoPermitidoException() {
        super("Os documentos de contratação ficam disponíveis assim que você for aprovado na vaga. "
            + "Enquanto isso, acompanhe o andamento em Minhas candidaturas.");
    }
}
