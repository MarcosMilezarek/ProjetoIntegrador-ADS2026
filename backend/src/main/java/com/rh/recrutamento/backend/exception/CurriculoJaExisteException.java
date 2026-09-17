package com.rh.recrutamento.backend.exception;

/** O candidato ja possui um curriculo cadastrado (relacao 1:1). Mapeada para 409 Conflict. */
public class CurriculoJaExisteException extends NegocioException {

    public CurriculoJaExisteException(Long usuarioId) {
        super("Usuario " + usuarioId + " ja possui um curriculo cadastrado.");
    }
}
