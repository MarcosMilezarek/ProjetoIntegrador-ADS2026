package com.rh.recrutamento.backend.usuario.exception;

import com.rh.recrutamento.backend.comum.exception.NegocioException;

/** E-mail já pertence a outro usuário. Mapeada para 409 Conflict. */
public class EmailJaCadastradoException extends NegocioException {

    public EmailJaCadastradoException(String email) {
        super("Já existe um usuário cadastrado com o e-mail " + email + ".");
    }
}
