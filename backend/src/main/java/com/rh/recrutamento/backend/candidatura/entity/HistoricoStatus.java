package com.rh.recrutamento.backend.candidatura.entity;

import com.rh.recrutamento.backend.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Trilha de cada mudanca de etapa de uma candidatura (UC09): quem mudou, de onde para onde e quando. */
@Entity
@Table(name = "historico_status")
@Getter
public class HistoricoStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidatura_id", nullable = false)
    private Candidatura candidatura;

    /** Nulo no registro da inscricao. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior")
    private Candidatura.Status statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false)
    private Candidatura.Status statusNovo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(length = 500)
    private String observacao;

    @CreationTimestamp
    @Column(name = "data_alteracao", nullable = false, updatable = false)
    private LocalDateTime dataAlteracao;

    protected HistoricoStatus() {
    }

    public HistoricoStatus(Candidatura candidatura, Candidatura.Status statusAnterior, Candidatura.Status statusNovo,
                           Usuario usuario, String observacao) {
        this.candidatura = candidatura;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.usuario = usuario;
        this.observacao = observacao;
    }
}
