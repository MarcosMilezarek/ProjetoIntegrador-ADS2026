package com.rh.recrutamento.backend.vaga.controller;

import com.rh.recrutamento.backend.auth.dto.UsuarioLogado;
import com.rh.recrutamento.backend.vaga.dto.request.VagaRequest;
import com.rh.recrutamento.backend.vaga.dto.request.VagaUpdateRequest;
import com.rh.recrutamento.backend.vaga.dto.response.VagaResponse;
import com.rh.recrutamento.backend.vaga.service.VagaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
    public ResponseEntity<VagaResponse> criar(@Valid @RequestBody VagaRequest request, @AuthenticationPrincipal Jwt jwt) {
        VagaResponse vaga = vagaService.criar(request, UsuarioLogado.de(jwt));
        return ResponseEntity.created(URI.create("/vagas/" + vaga.id())).body(vaga);
    }

    @GetMapping
    public ResponseEntity<List<VagaResponse>> listar(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(vagaService.listar(UsuarioLogado.de(jwt)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VagaResponse> buscarPorId(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(vagaService.buscarPorId(id, UsuarioLogado.de(jwt)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VagaResponse> atualizar(@PathVariable Long id,
                                                   @Valid @RequestBody VagaUpdateRequest request,
                                                   @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(vagaService.atualizar(id, request, UsuarioLogado.de(jwt)));
    }
}
