package com.rh.recrutamento.backend.usuario.exception;

import com.rh.recrutamento.backend.comum.exception.NegocioException;

/** E-mail já pertence a outro usuário. Mapeada para 409 Conflict. */
public class EmailJaCadastradoException extends NegocioException {

    public EmailJaCadastradoException(String email) {
        super("O e-mail " + email + " já tem cadastro. Entre com ele ou use outro e-mail para criar a sua conta.");
    }
}
