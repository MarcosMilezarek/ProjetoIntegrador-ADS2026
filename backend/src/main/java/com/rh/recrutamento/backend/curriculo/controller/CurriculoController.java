package com.rh.recrutamento.backend.curriculo.controller;

import com.rh.recrutamento.backend.auth.dto.UsuarioLogado;
import com.rh.recrutamento.backend.curriculo.dto.request.CurriculoRequest;
import com.rh.recrutamento.backend.curriculo.dto.request.CurriculoUpdateRequest;
import com.rh.recrutamento.backend.curriculo.dto.response.CurriculoResponse;
import com.rh.recrutamento.backend.curriculo.service.CurriculoService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/curriculos")
public class CurriculoController {

    private final CurriculoService curriculoService;

    public CurriculoController(CurriculoService curriculoService) {
        this.curriculoService = curriculoService;
    }

    @PostMapping
    public ResponseEntity<CurriculoResponse> criar(@Valid @RequestBody CurriculoRequest request,
                                                   @AuthenticationPrincipal Jwt jwt) {
        CurriculoResponse curriculo = curriculoService.criar(request, UsuarioLogado.de(jwt));
        return ResponseEntity.created(URI.create("/curriculos/" + curriculo.id())).body(curriculo);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CurriculoResponse> buscarPorId(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(curriculoService.buscarPorId(id, UsuarioLogado.de(jwt)));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<CurriculoResponse> buscarPorUsuario(@PathVariable Long usuarioId,
                                                              @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(curriculoService.buscarPorUsuario(usuarioId, UsuarioLogado.de(jwt)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CurriculoResponse> atualizar(@PathVariable Long id,
                                                         @Valid @RequestBody CurriculoUpdateRequest request,
                                                         @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(curriculoService.atualizar(id, request, UsuarioLogado.de(jwt)));
    }

    @PostMapping(path = "/{id}/arquivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CurriculoResponse> anexarArquivo(@PathVariable Long id,
                                                            @RequestPart("arquivo") MultipartFile arquivo,
                                                            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(curriculoService.anexarArquivo(id, arquivo, UsuarioLogado.de(jwt)));
    }

    @GetMapping("/{id}/arquivo")
    public ResponseEntity<byte[]> baixarArquivo(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        CurriculoService.ArquivoBaixado arquivo = curriculoService.baixarArquivo(id, UsuarioLogado.de(jwt));
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(arquivo.contentType()))
            .header("Content-Disposition", ContentDisposition.attachment()
                .filename(arquivo.nomeOriginal(), StandardCharsets.UTF_8).build().toString())
            .body(arquivo.conteudo());
    }
}
