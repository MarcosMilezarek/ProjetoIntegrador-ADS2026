package com.rh.recrutamento.backend.exception;

/** Recurso inexistente. Mapeada para 404 Not Found. */
public class RecursoNaoEncontradoException extends NegocioException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
