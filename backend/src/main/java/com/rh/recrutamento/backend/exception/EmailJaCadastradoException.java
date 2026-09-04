package com.rh.recrutamento.backend.exception;

/** E-mail já pertence a outro usuário. Mapeada para 409 Conflict. */
public class EmailJaCadastradoException extends NegocioException {

    public EmailJaCadastradoException(String email) {
        super("Já existe um usuário cadastrado com o e-mail " + email + ".");
    }
}
