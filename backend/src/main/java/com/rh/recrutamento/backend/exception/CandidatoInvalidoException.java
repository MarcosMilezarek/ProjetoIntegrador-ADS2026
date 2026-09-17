package com.rh.recrutamento.backend.exception;

/** O usuario informado nao possui perfil de candidato. Mapeada para 403 Forbidden. */
public class CandidatoInvalidoException extends NegocioException {

    public CandidatoInvalidoException(Long usuarioId) {
        super("Usuario " + usuarioId + " nao possui perfil de candidato.");
    }
}
