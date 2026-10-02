package com.rh.recrutamento.backend.documento.controller;

import com.rh.recrutamento.backend.auth.dto.UsuarioLogado;
import com.rh.recrutamento.backend.documento.dto.response.DocumentoResponse;
import com.rh.recrutamento.backend.documento.dto.response.DocumentosDaCandidaturaResponse;
import com.rh.recrutamento.backend.documento.dto.response.TipoDocumentoResponse;
import com.rh.recrutamento.backend.documento.entity.Documento;
import com.rh.recrutamento.backend.documento.service.DocumentoService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
public class DocumentoController {

    private final DocumentoService documentoService;

    public DocumentoController(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    /** Lista fechada de tipos de documento (o upload recebe o codigo). */
    @GetMapping("/documentos/tipos")
    public ResponseEntity<List<TipoDocumentoResponse>> listarTipos() {
        return ResponseEntity.ok(documentoService.listarTipos());
    }

    /** Quadro de enviados e pendentes da candidatura, igual para o candidato e para o RH. */
    @GetMapping("/candidaturas/{candidaturaId}/documentos")
    public ResponseEntity<DocumentosDaCandidaturaResponse> situacao(@PathVariable Long candidaturaId,
                                                                    @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(documentoService.situacao(candidaturaId, UsuarioLogado.de(jwt)));
    }

    /**
     * O candidato envia um documento (PDF ou DOCX, ate 5MB) de uma candidatura aprovada. "tipo" e o
     * codigo da lista (GET /documentos/tipos); reenviar o mesmo tipo substitui o anterior.
     */
    @PostMapping(path = "/candidaturas/{candidaturaId}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoResponse> enviar(@PathVariable Long candidaturaId,
                                                    @RequestParam("tipo") String tipo,
                                                    @RequestPart("arquivo") MultipartFile arquivo,
                                                    @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(documentoService.enviar(candidaturaId, tipo, arquivo, UsuarioLogado.de(jwt)));
    }

    /** Painel do RH: aprova o documento; o candidato e avisado. */
    @PutMapping("/documentos/{id}/aprovar")
    public ResponseEntity<DocumentoResponse> aprovar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(documentoService.avaliar(id, Documento.Status.aprovado, UsuarioLogado.de(jwt)));
    }

    /** Painel do RH: recusa o documento; o candidato e avisado e pode enviar uma nova versao. */
    @PutMapping("/documentos/{id}/recusar")
    public ResponseEntity<DocumentoResponse> recusar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(documentoService.avaliar(id, Documento.Status.recusado, UsuarioLogado.de(jwt)));
    }

    @GetMapping("/documentos")
    public ResponseEntity<List<DocumentoResponse>> listar(@RequestParam(required = false) Long candidatoId,
                                                          @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(documentoService.listar(candidatoId, UsuarioLogado.de(jwt)));
    }

    @GetMapping("/documentos/{id}/arquivo")
    public ResponseEntity<byte[]> baixar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        DocumentoService.DocumentoBaixado arquivo = documentoService.baixar(id, UsuarioLogado.de(jwt));
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(arquivo.contentType()))
            .header("Content-Disposition", ContentDisposition.attachment()
                .filename(arquivo.nome(), StandardCharsets.UTF_8).build().toString())
            .body(arquivo.conteudo());
    }
}
