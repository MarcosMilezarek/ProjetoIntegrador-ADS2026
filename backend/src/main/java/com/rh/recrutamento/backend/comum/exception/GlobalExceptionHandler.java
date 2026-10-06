package com.rh.recrutamento.backend.comum.exception;

import com.rh.recrutamento.backend.auth.exception.CredenciaisInvalidasException;
import com.rh.recrutamento.backend.auth.exception.UsuarioInativoException;
import com.rh.recrutamento.backend.candidatura.exception.CandidaturaNaoPermitidaException;
import com.rh.recrutamento.backend.comum.dto.ErroResponse;
import com.rh.recrutamento.backend.curriculo.exception.CandidatoInvalidoException;
import com.rh.recrutamento.backend.curriculo.exception.CurriculoJaExisteException;
import com.rh.recrutamento.backend.documento.exception.DocumentoNaoPermitidoException;
import com.rh.recrutamento.backend.usuario.exception.EmailJaCadastradoException;
import com.rh.recrutamento.backend.vaga.exception.RhInvalidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Tratamento central de erros: converte exceções em respostas HTTP padronizadas. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratarNaoEncontrado(RecursoNaoEncontradoException ex) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<ErroResponse> tratarEmailDuplicado(EmailJaCadastradoException ex) {
        return construir(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResponse> tratarCredenciaisInvalidas(CredenciaisInvalidasException ex) {
        return construir(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(UsuarioInativoException.class)
    public ResponseEntity<ErroResponse> tratarUsuarioInativo(UsuarioInativoException ex) {
        return construir(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(RhInvalidoException.class)
    public ResponseEntity<ErroResponse> tratarRhInvalido(RhInvalidoException ex) {
        return construir(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroResponse> tratarAcessoNegado(AcessoNegadoException ex) {
        return construir(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(CandidatoInvalidoException.class)
    public ResponseEntity<ErroResponse> tratarCandidatoInvalido(CandidatoInvalidoException ex) {
        return construir(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(CurriculoJaExisteException.class)
    public ResponseEntity<ErroResponse> tratarCurriculoDuplicado(CurriculoJaExisteException ex) {
        return construir(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(CandidaturaNaoPermitidaException.class)
    public ResponseEntity<ErroResponse> tratarCandidaturaNaoPermitida(CandidaturaNaoPermitidaException ex) {
        return construir(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Ex.: excluir usuario que ainda tem vagas ou candidaturas (FK RESTRICT). Antes virava 500. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> tratarViolacaoDeIntegridade(DataIntegrityViolationException ex) {
        return construir(HttpStatus.CONFLICT,
            "A operação conflita com registros vinculados. Para desativar um usuário, altere o status em vez de excluir.");
    }

    @ExceptionHandler(DocumentoNaoPermitidoException.class)
    public ResponseEntity<ErroResponse> tratarDocumentoNaoPermitido(DocumentoNaoPermitidoException ex) {
        return construir(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Multipart sem o arquivo ou sem o campo tipo: 400, nao 500. */
    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErroResponse> tratarParteAusente(Exception ex) {
        return construir(HttpStatus.BAD_REQUEST,
            "Faltou alguma informação no envio. Confira os dados e tente novamente.");
    }

    /** Valor de parametro fora do dominio (ex.: GET /agenda?status=talvez). Antes virava 500. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tratarParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return construir(HttpStatus.BAD_REQUEST, "O valor informado para \"" + ex.getName() + "\" não é válido. Confira e tente novamente.");
    }

    @ExceptionHandler(ArquivoInvalidoException.class)
    public ResponseEntity<ErroResponse> tratarArquivoInvalido(ArquivoInvalidoException ex) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Estouro do limite de multipart do servidor, antes de chegar na validacao de negocio. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErroResponse> tratarArquivoGrande(MaxUploadSizeExceededException ex) {
        return construir(HttpStatus.PAYLOAD_TOO_LARGE,
            "O arquivo ultrapassa o limite de 5MB. Reduza o tamanho (por exemplo, comprimindo o PDF) e envie novamente.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(ErroResponse.de(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            "Alguns campos precisam de ajuste. Confira os itens indicados e tente novamente.",
            campos
        ));
    }

    /** JSON malformado ou valor fora do domínio de um enum (perfil/status). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarCorpoIlegivel(HttpMessageNotReadableException ex) {
        return construir(HttpStatus.BAD_REQUEST,
            "Não conseguimos entender os dados enviados. Revise as informações e tente novamente.");
    }

    /** Rota que nao existe (cai no handler de recursos estaticos): 404, nao 500. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> tratarRotaInexistente(NoResourceFoundException ex) {
        return construir(HttpStatus.NOT_FOUND, "Recurso nao encontrado.");
    }

    /** Verbo que a rota nao aceita (ex.: PATCH /vagas/1): 405, nao 500. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> tratarMetodoNaoSuportado(HttpRequestMethodNotSupportedException ex) {
        return construir(HttpStatus.METHOD_NOT_ALLOWED, "Esta operação não está disponível para este endereço.");
    }

    /** Corpo enviado em formato que a rota nao le (ex.: text/plain em rota JSON): 415, nao 500. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResponse> tratarTipoNaoSuportado(HttpMediaTypeNotSupportedException ex) {
        return construir(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "O formato dos dados enviados não é aceito por esta operação.");
    }

    /** O cliente fechou a conexao (ex.: aba com o stream de notificacoes fechada): nao ha a quem responder. */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void tratarClienteDesconectado() {
        // nada a fazer; a conexao ja foi removida pelo NotificacaoService
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarErroInesperado(Exception ex) {
        log.error("Erro não tratado", ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR,
            "Algo não saiu como esperado do nosso lado. Tente novamente em alguns instantes.");
    }

    private ResponseEntity<ErroResponse> construir(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status)
            .body(ErroResponse.de(status.value(), status.getReasonPhrase(), mensagem));
    }
}
