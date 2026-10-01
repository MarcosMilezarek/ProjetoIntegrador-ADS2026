package com.rh.recrutamento.backend.auth.controller;

import com.rh.recrutamento.backend.auth.dto.UsuarioLogado;
import com.rh.recrutamento.backend.auth.dto.request.LoginRequest;
import com.rh.recrutamento.backend.auth.dto.response.LoginResponse;
import com.rh.recrutamento.backend.auth.service.AuthService;
import com.rh.recrutamento.backend.usuario.dto.response.UsuarioResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.autenticar(request));
    }

    /** Restaura a sessao ao recarregar a pagina: valida o token guardado e devolve quem esta logado. */
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> usuarioAtual(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(authService.usuarioAtual(UsuarioLogado.de(jwt)));
    }
}
