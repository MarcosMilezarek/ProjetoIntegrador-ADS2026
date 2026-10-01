package com.rh.recrutamento.backend.notificacao.entity;

import com.rh.recrutamento.backend.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Aviso para um usuario (candidato ou RH) sobre algo que aconteceu no processo seletivo. */
@Entity
@Table(name = "notificacao")
@Getter
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Destinatario: so ele lista, recebe e marca como lida. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 500)
    private String mensagem;

    @Column(nullable = false)
    private boolean lida;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    protected Notificacao() {
    }

    public Notificacao(Usuario usuario, String titulo, String mensagem) {
        this.usuario = usuario;
        this.titulo = titulo;
        this.mensagem = mensagem;
    }

    public void marcarComoLida() {
        this.lida = true;
    }
}
