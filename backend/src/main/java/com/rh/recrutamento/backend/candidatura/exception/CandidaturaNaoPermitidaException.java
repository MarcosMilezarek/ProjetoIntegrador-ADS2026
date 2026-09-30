package com.rh.recrutamento.backend.candidatura.exception;

import com.rh.recrutamento.backend.comum.exception.NegocioException;

/** Candidatura barrada por regra: vaga nao aberta (RN05), sem curriculo (UC04) ou repetida (RN01). Mapeada para 409 Conflict. */
public class CandidaturaNaoPermitidaException extends NegocioException {

    public CandidaturaNaoPermitidaException(String mensagem) {
        super(mensagem);
    }
}
