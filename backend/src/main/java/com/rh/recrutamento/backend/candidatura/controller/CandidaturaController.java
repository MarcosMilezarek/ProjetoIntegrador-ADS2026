package com.rh.recrutamento.backend.candidatura.controller;

import com.rh.recrutamento.backend.auth.dto.UsuarioLogado;
import com.rh.recrutamento.backend.candidatura.dto.request.CandidaturaRequest;
import com.rh.recrutamento.backend.candidatura.dto.request.EntrevistaRequest;
import com.rh.recrutamento.backend.candidatura.dto.request.StatusCandidaturaRequest;
import com.rh.recrutamento.backend.candidatura.dto.response.CandidaturaResponse;
import com.rh.recrutamento.backend.candidatura.service.CandidaturaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CandidaturaController {

    private final CandidaturaService candidaturaService;

    public CandidaturaController(CandidaturaService candidaturaService) {
        this.candidaturaService = candidaturaService;
    }

    @PostMapping("/candidaturas")
    public ResponseEntity<CandidaturaResponse> candidatar(@Valid @RequestBody CandidaturaRequest request,
                                                          @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(candidaturaService.candidatar(request, UsuarioLogado.de(jwt)));
    }

    @GetMapping("/candidaturas/minhas")
    public ResponseEntity<List<CandidaturaResponse>> listarMinhas(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(candidaturaService.listarMinhas(UsuarioLogado.de(jwt)));
    }

    /** Painel do RH: inscritos de uma vaga. */
    @GetMapping("/vagas/{vagaId}/candidaturas")
    public ResponseEntity<List<CandidaturaResponse>> listarPorVaga(@PathVariable Long vagaId,
                                                                   @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(candidaturaService.listarPorVaga(vagaId, UsuarioLogado.de(jwt)));
    }

    /** Painel do RH: muda a etapa do candidato na vaga e registra no historico. */
    @PutMapping("/candidaturas/{id}/status")
    public ResponseEntity<CandidaturaResponse> alterarStatus(@PathVariable Long id,
                                                             @Valid @RequestBody StatusCandidaturaRequest request,
                                                             @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(candidaturaService.alterarStatus(id, request, UsuarioLogado.de(jwt)));
    }

    /** Painel do RH: confirma a entrevista com data e hora; o candidato e avisado na hora. */
    @PutMapping("/candidaturas/{id}/entrevista")
    public ResponseEntity<CandidaturaResponse> agendarEntrevista(@PathVariable Long id,
                                                                 @Valid @RequestBody EntrevistaRequest request,
                                                                 @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(candidaturaService.agendarEntrevista(id, request, UsuarioLogado.de(jwt)));
    }
}
