package com.rh.recrutamento.backend.exception;

/** Arquivo enviado nao atende as regras (formato, tamanho ou vazio). */
public class ArquivoInvalidoException extends NegocioException {

    public ArquivoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
