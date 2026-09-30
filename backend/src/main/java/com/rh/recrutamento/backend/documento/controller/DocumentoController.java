package com.rh.recrutamento.backend.documento.controller;

import com.rh.recrutamento.backend.auth.dto.UsuarioLogado;
import com.rh.recrutamento.backend.documento.dto.response.DocumentoResponse;
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

    /** O candidato envia um documento (PDF ou DOCX, ate 5MB) de uma candidatura aprovada. */
    @PostMapping(path = "/candidaturas/{candidaturaId}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoResponse> enviar(@PathVariable Long candidaturaId,
                                                    @RequestParam("tipo") String tipo,
                                                    @RequestPart("arquivo") MultipartFile arquivo,
                                                    @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(documentoService.enviar(candidaturaId, tipo, arquivo, UsuarioLogado.de(jwt)));
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
