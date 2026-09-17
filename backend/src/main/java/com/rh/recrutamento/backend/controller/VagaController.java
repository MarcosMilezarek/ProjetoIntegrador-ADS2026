package com.rh.recrutamento.backend.controller;

import com.rh.recrutamento.backend.dto.vaga.request.VagaRequest;
import com.rh.recrutamento.backend.dto.vaga.request.VagaUpdateRequest;
import com.rh.recrutamento.backend.dto.vaga.response.VagaResponse;
import com.rh.recrutamento.backend.service.VagaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/vagas")
public class VagaController {

    private final VagaService vagaService;

    public VagaController(VagaService vagaService) {
        this.vagaService = vagaService;
    }

    @PostMapping
    public ResponseEntity<VagaResponse> criar(@Valid @RequestBody VagaRequest request) {
        VagaResponse vaga = vagaService.criar(request);
        return ResponseEntity.created(URI.create("/vagas/" + vaga.id())).body(vaga);
    }

    @GetMapping
    public ResponseEntity<List<VagaResponse>> listar() {
        return ResponseEntity.ok(vagaService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VagaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(vagaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VagaResponse> atualizar(@PathVariable Long id,
                                                   @Valid @RequestBody VagaUpdateRequest request) {
        return ResponseEntity.ok(vagaService.atualizar(id, request));
    }
}
