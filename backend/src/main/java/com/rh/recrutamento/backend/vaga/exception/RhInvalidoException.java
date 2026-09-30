package com.rh.recrutamento.backend.vaga.exception;

import com.rh.recrutamento.backend.comum.exception.NegocioException;

/** O usuário informado como responsável pela vaga não possui perfil de RH (RN02). Mapeada para 403 Forbidden. */
public class RhInvalidoException extends NegocioException {

    public RhInvalidoException(Long usuarioId) {
        super("Usuário " + usuarioId + " não possui perfil de RH.");
    }
}
