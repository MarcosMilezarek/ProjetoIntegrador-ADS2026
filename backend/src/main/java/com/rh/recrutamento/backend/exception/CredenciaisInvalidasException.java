package com.rh.recrutamento.backend.exception;

/** E-mail ou senha incorretos. Mapeada para 401 Unauthorized. */
public class CredenciaisInvalidasException extends NegocioException {

    public CredenciaisInvalidasException() {
        // Mensagem genérica de propósito: não revela se o erro foi no e-mail ou na senha (RNF02/segurança)
        super("E-mail ou senha inválidos.");
    }
}
