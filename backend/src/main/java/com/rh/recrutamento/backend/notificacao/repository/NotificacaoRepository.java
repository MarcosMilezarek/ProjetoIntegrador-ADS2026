package com.rh.recrutamento.backend.notificacao.repository;

import com.rh.recrutamento.backend.notificacao.entity.Notificacao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findByUsuario_IdOrderByIdDesc(Long usuarioId);

    @Modifying
    @Query("update Notificacao n set n.lida = true where n.usuario.id = :usuarioId and n.lida = false")
    int marcarTodasComoLidas(Long usuarioId);

    /** Maior id gravado ate agora (0 com a tabela vazia): ponto de partida do sincronizador de notificacoes. */
    @Query("select coalesce(max(n.id), 0) from Notificacao n")
    long maiorId();

    /** Notificacoes gravadas depois de um id, das mais antigas para as mais novas, ja com o destinatario carregado. */
    @Query("select n from Notificacao n join fetch n.usuario where n.id > :id order by n.id")
    List<Notificacao> buscarDepoisDe(long id, Pageable pageable);
}
