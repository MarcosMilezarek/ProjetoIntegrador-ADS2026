package com.rh.recrutamento.backend.controller;

import com.rh.recrutamento.backend.dto.curriculo.request.CurriculoRequest;
import com.rh.recrutamento.backend.dto.curriculo.request.CurriculoUpdateRequest;
import com.rh.recrutamento.backend.dto.curriculo.response.CurriculoResponse;
import com.rh.recrutamento.backend.service.CurriculoService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<CurriculoResponse> criar(@Valid @RequestBody CurriculoRequest request) {
        CurriculoResponse curriculo = curriculoService.criar(request);
        return ResponseEntity.created(URI.create("/curriculos/" + curriculo.id())).body(curriculo);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CurriculoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(curriculoService.buscarPorId(id));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<CurriculoResponse> buscarPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(curriculoService.buscarPorUsuario(usuarioId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CurriculoResponse> atualizar(@PathVariable Long id,
                                                         @Valid @RequestBody CurriculoUpdateRequest request) {
        return ResponseEntity.ok(curriculoService.atualizar(id, request));
    }

    @PostMapping(path = "/{id}/arquivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CurriculoResponse> anexarArquivo(@PathVariable Long id,
                                                            @RequestPart("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(curriculoService.anexarArquivo(id, arquivo));
    }

    @GetMapping("/{id}/arquivo")
    public ResponseEntity<byte[]> baixarArquivo(@PathVariable Long id) {
        CurriculoService.ArquivoBaixado arquivo = curriculoService.baixarArquivo(id);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(arquivo.contentType()))
            .header("Content-Disposition", ContentDisposition.attachment()
                .filename(arquivo.nomeOriginal(), StandardCharsets.UTF_8).build().toString())
            .body(arquivo.conteudo());
    }
}
