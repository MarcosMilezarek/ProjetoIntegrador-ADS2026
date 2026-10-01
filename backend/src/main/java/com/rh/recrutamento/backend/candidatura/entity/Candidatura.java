package com.rh.recrutamento.backend.candidatura.entity;

import com.rh.recrutamento.backend.usuario.entity.Usuario;
import com.rh.recrutamento.backend.vaga.entity.Vaga;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Inscricao de um candidato em uma vaga (RF07). RN01: uma por candidato e vaga. */
@Entity
@Table(name = "candidatura",
    uniqueConstraints = @UniqueConstraint(name = "uk_candidatura_usuario_vaga", columnNames = {"usuario_id", "vaga_id"}))
@Getter
public class Candidatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario candidato;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vaga_id", nullable = false)
    private Vaga vaga;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    /** Data e hora da entrevista marcada pelo RH, em UTC. */
    @Column(name = "entrevista_em")
    private LocalDateTime entrevistaEm;

    @CreationTimestamp
    @Column(name = "data_candidatura", nullable = false, updatable = false)
    private LocalDateTime dataCandidatura;

    protected Candidatura() {
    }

    public Candidatura(Usuario candidato, Vaga vaga) {
        this.candidato = candidato;
        this.vaga = vaga;
        this.status = Status.inscrito;
    }

    public void alterarStatus(Status novo) {
        this.status = novo;
    }

    public void agendarEntrevista(Instant quando) {
        this.entrevistaEm = LocalDateTime.ofInstant(quando, ZoneOffset.UTC);
    }

    /**
     * Etapas do processo seletivo, na ordem usual: inscrito, em_triagem, entrevista e aprovado
     * (ou reprovado); contratado e cancelado encerram. A ordem nao e imposta: o RH pode corrigir
     * um passo, e todo passo fica em historico_status.
     */
    public enum Status {
        inscrito("Inscrito"),
        em_triagem("Em análise"),
        entrevista("Entrevista"),
        aprovado("Aprovado"),
        reprovado("Não selecionado"),
        contratado("Contratado"),
        cancelado("Cancelado");

        /** Nome mostrado ao candidato nas notificacoes, igual ao do portal. */
        private final String rotulo;

        Status(String rotulo) {
            this.rotulo = rotulo;
        }

        public String getRotulo() {
            return rotulo;
        }
    }
}
