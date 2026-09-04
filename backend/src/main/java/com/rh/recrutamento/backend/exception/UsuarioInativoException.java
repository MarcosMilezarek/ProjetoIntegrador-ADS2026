package com.rh.recrutamento.backend.exception;

/** Usuário existe e a senha confere, mas o acesso está suspenso. Mapeada para 403 Forbidden. */
public class UsuarioInativoException extends NegocioException {

    public UsuarioInativoException() {
        super("Usuário bloqueado ou inativo.");
    }
}
