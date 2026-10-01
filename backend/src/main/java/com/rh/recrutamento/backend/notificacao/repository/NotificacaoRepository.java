package com.rh.recrutamento.backend.notificacao.repository;

import com.rh.recrutamento.backend.notificacao.entity.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findByUsuario_IdOrderByIdDesc(Long usuarioId);

    @Modifying
    @Query("update Notificacao n set n.lida = true where n.usuario.id = :usuarioId and n.lida = false")
    int marcarTodasComoLidas(Long usuarioId);
}
