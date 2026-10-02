package com.rh.recrutamento.backend.funcionario.controller;

import com.rh.recrutamento.backend.auth.dto.UsuarioLogado;
import com.rh.recrutamento.backend.funcionario.dto.response.FuncionarioPerfilResponse;
import com.rh.recrutamento.backend.funcionario.dto.response.FuncionarioResponse;
import com.rh.recrutamento.backend.funcionario.service.FuncionarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Painel do RH: funcionarios contratados. A contratacao e POST /candidaturas/{id}/contratar. */
@RestController
@RequestMapping("/funcionarios")
public class FuncionarioController {

    private final FuncionarioService funcionarioService;

    public FuncionarioController(FuncionarioService funcionarioService) {
        this.funcionarioService = funcionarioService;
    }

    @GetMapping
    public ResponseEntity<List<FuncionarioResponse>> listar(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(funcionarioService.listar(UsuarioLogado.de(jwt)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FuncionarioPerfilResponse> perfil(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(funcionarioService.perfil(id, UsuarioLogado.de(jwt)));
    }

    @PutMapping("/{id}/inativar")
    public ResponseEntity<FuncionarioResponse> inativar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(funcionarioService.inativar(id, UsuarioLogado.de(jwt)));
    }
}
