package com.rh.recrutamento.backend.comum.exception;

/** Raiz das exceções de regra de negócio tratadas pelo {@link GlobalExceptionHandler}. */
public abstract class NegocioException extends RuntimeException {

    protected NegocioException(String mensagem) {
        super(mensagem);
    }
}
