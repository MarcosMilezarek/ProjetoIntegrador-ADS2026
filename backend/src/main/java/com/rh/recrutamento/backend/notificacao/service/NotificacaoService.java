package com.rh.recrutamento.backend.notificacao.service;

import com.rh.recrutamento.backend.auth.dto.UsuarioLogado;
import com.rh.recrutamento.backend.comum.exception.AcessoNegadoException;
import com.rh.recrutamento.backend.comum.exception.RecursoNaoEncontradoException;
import com.rh.recrutamento.backend.notificacao.dto.response.NotificacaoResponse;
import com.rh.recrutamento.backend.notificacao.entity.Notificacao;
import com.rh.recrutamento.backend.notificacao.mapper.NotificacaoMapper;
import com.rh.recrutamento.backend.notificacao.repository.NotificacaoRepository;
import com.rh.recrutamento.backend.usuario.entity.Usuario;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Notificacoes gravadas no banco e entregues na hora, por SSE, a quem estiver conectado.
 * As conexoes abertas ficam em memoria, por usuario (uma por aba). O destinatario vem
 * sempre do token: ninguem escuta, lista ou marca notificacao de outra pessoa.
 */
@Service
@Transactional(readOnly = true)
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final NotificacaoMapper notificacaoMapper;
    private final Map<Long, Set<SseEmitter>> conexoes = new ConcurrentHashMap<>();

    public NotificacaoService(NotificacaoRepository notificacaoRepository, NotificacaoMapper notificacaoMapper) {
        this.notificacaoRepository = notificacaoRepository;
        this.notificacaoMapper = notificacaoMapper;
    }

    /**
     * Unico ponto de criacao de notificacoes. Grava na transacao de quem chamou e so envia depois
     * do commit, para ninguem ser avisado de uma acao que acabou desfeita. "referenciaId" e o id da
     * candidatura (tipo candidatura) ou da vaga (tipo nova_vaga).
     */
    @Transactional
    public void notificar(Usuario destinatario, Notificacao.Tipo tipo, Long referenciaId, String titulo, String mensagem) {
        Notificacao notificacao = notificacaoRepository.save(
            new Notificacao(destinatario, tipo, referenciaId, titulo, mensagem));
        NotificacaoResponse resposta = notificacaoMapper.toResponse(notificacao);
        Long destinatarioId = destinatario.getId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviar(destinatarioId, resposta);
                }
            });
        } else {
            enviar(destinatarioId, resposta);
        }
    }

    public List<NotificacaoResponse> listar(UsuarioLogado logado) {
        return notificacaoRepository.findByUsuario_IdOrderByIdDesc(logado.id()).stream()
            .map(notificacaoMapper::toResponse)
            .toList();
    }

    @Transactional
    public NotificacaoResponse marcarComoLida(Long id, UsuarioLogado logado) {
        Notificacao notificacao = notificacaoRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Notificação " + id + " não encontrada."));
        if (!notificacao.getUsuario().getId().equals(logado.id())) {
            throw new AcessoNegadoException("Esta notificação é de outro usuário.");
        }
        notificacao.marcarComoLida();
        return notificacaoMapper.toResponse(notificacao);
    }

    @Transactional
    public void marcarTodasComoLidas(UsuarioLogado logado) {
        notificacaoRepository.marcarTodasComoLidas(logado.id());
    }

    /** Abre a conexao SSE do usuario logado. Ela fecha quando o token expira; o cliente reconecta com um novo. */
    public SseEmitter conectar(UsuarioLogado logado, Instant tokenExpiraEm) {
        long validadeRestante = Math.max(1, Duration.between(Instant.now(), tokenExpiraEm).toMillis());
        SseEmitter emissor = new SseEmitter(validadeRestante);
        Set<SseEmitter> doUsuario = conexoes.computeIfAbsent(logado.id(), id -> ConcurrentHashMap.newKeySet());
        doUsuario.add(emissor);
        Runnable remover = () -> doUsuario.remove(emissor);
        emissor.onCompletion(remover);
        emissor.onTimeout(remover);
        emissor.onError(erro -> remover.run());
        try {
            // primeiro evento: confirma a conexao e faz o navegador receber os cabecalhos na hora
            emissor.send(SseEmitter.event().name("conectado").data("ok"));
        } catch (IOException e) {
            remover.run();
        }
        return emissor;
    }

    /**
     * Comentario SSE a cada 25s: mantem a conexao viva atras do nginx (que corta apos 60s sem trafego)
     * e descarta as abas que ja fecharam.
     */
    @Scheduled(fixedRate = 25_000)
    void manterConexoes() {
        conexoes.forEach((usuarioId, doUsuario) -> doUsuario.forEach(emissor -> {
            try {
                emissor.send(SseEmitter.event().comment("ping"));
            } catch (IOException | IllegalStateException e) {
                doUsuario.remove(emissor);
            }
        }));
    }

    private void enviar(Long usuarioId, NotificacaoResponse notificacao) {
        Set<SseEmitter> doUsuario = conexoes.getOrDefault(usuarioId, Set.of());
        for (SseEmitter emissor : doUsuario) {
            try {
                emissor.send(SseEmitter.event().name("notificacao").id(notificacao.id().toString()).data(notificacao));
            } catch (IOException | IllegalStateException e) {
                doUsuario.remove(emissor);
            }
        }
    }
}
