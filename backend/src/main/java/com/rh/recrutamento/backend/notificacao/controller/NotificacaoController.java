package com.rh.recrutamento.backend.notificacao.controller;

import com.rh.recrutamento.backend.auth.dto.UsuarioLogado;
import com.rh.recrutamento.backend.notificacao.dto.response.NotificacaoResponse;
import com.rh.recrutamento.backend.notificacao.service.NotificacaoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/** Notificacoes do usuario logado (candidato, RH ou administrador). */
@RestController
@RequestMapping("/notificacoes")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @GetMapping
    public ResponseEntity<List<NotificacaoResponse>> listar(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(notificacaoService.listar(UsuarioLogado.de(jwt)));
    }

    /**
     * Tempo real por SSE. O token vai no cabecalho Authorization, como nas demais rotas: o cliente
     * le o fluxo com fetch, e nao com EventSource, para o token nao aparecer na URL.
     */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> conectar(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok()
            // sem isso o nginx segura os eventos em buffer e eles nao chegam na hora
            .header("X-Accel-Buffering", "no")
            .header("Cache-Control", "no-cache")
            .body(notificacaoService.conectar(UsuarioLogado.de(jwt), jwt.getExpiresAt()));
    }

    @PutMapping("/lidas")
    public ResponseEntity<Void> marcarTodasComoLidas(@AuthenticationPrincipal Jwt jwt) {
        notificacaoService.marcarTodasComoLidas(UsuarioLogado.de(jwt));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/lida")
    public ResponseEntity<NotificacaoResponse> marcarComoLida(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(notificacaoService.marcarComoLida(id, UsuarioLogado.de(jwt)));
    }
}
