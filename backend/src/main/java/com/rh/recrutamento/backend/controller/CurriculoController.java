package com.rh.recrutamento.backend.controller;

import com.rh.recrutamento.backend.dto.curriculo.request.CurriculoRequest;
import com.rh.recrutamento.backend.dto.curriculo.request.CurriculoUpdateRequest;
import com.rh.recrutamento.backend.dto.curriculo.response.CurriculoResponse;
import com.rh.recrutamento.backend.service.CurriculoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

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
}
