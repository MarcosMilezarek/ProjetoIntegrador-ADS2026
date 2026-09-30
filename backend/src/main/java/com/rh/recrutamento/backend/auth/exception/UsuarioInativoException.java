package com.rh.recrutamento.backend.auth.exception;

import com.rh.recrutamento.backend.comum.exception.NegocioException;

/** Usuário existe e a senha confere, mas o acesso está suspenso. Mapeada para 403 Forbidden. */
public class UsuarioInativoException extends NegocioException {

    public UsuarioInativoException() {
        super("Usuário bloqueado ou inativo.");
    }
}
