package com.rh.recrutamento.backend.exception;

/** O recurso existe, mas pertence a outra pessoa (ou a vaga e de outro RH). Mapeada para 403 Forbidden. */
public class AcessoNegadoException extends NegocioException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
