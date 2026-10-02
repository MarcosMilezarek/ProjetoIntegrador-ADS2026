package com.rh.recrutamento.backend.auth.exception;

import com.rh.recrutamento.backend.comum.exception.NegocioException;

/** E-mail ou senha incorretos. Mapeada para 401 Unauthorized. */
public class CredenciaisInvalidasException extends NegocioException {

    public CredenciaisInvalidasException() {
        // Mensagem genérica de propósito: não revela se o erro foi no e-mail ou na senha (RNF02/segurança)
        super("Não foi possível entrar: o e-mail ou a senha não conferem. Confira os dados e tente novamente.");
    }
}
