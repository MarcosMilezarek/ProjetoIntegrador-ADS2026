package com.rh.recrutamento.backend.controller;

import com.rh.recrutamento.backend.dto.auth.UsuarioLogado;
import com.rh.recrutamento.backend.dto.usuario.request.UsuarioRequest;
import com.rh.recrutamento.backend.dto.usuario.request.UsuarioUpdateRequest;
import com.rh.recrutamento.backend.dto.usuario.response.UsuarioResponse;
import com.rh.recrutamento.backend.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** Publico para o cadastro de candidato; com token de administrador cadastra tambem RH e administradores. */
    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioRequest request,
                                                 @AuthenticationPrincipal Jwt jwt) {
        UsuarioResponse usuario = usuarioService.criar(request, jwt != null ? UsuarioLogado.de(jwt) : null);
        return ResponseEntity.created(URI.create("/usuarios/" + usuario.id())).body(usuario);
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id, UsuarioLogado.de(jwt)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizar(@PathVariable Long id,
                                                     @Valid @RequestBody UsuarioUpdateRequest request,
                                                     @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(usuarioService.atualizar(id, request, UsuarioLogado.de(jwt)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        usuarioService.excluir(id, UsuarioLogado.de(jwt));
        return ResponseEntity.noContent().build();
    }
}
