package com.rh.recrutamento.backend.candidatura.entity;

import com.rh.recrutamento.backend.usuario.entity.Usuario;
import com.rh.recrutamento.backend.vaga.entity.Vaga;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

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

    /**
     * Etapas do processo seletivo, na ordem usual: inscrito, em_triagem, entrevista e aprovado
     * (ou reprovado); contratado e cancelado encerram. A ordem nao e imposta: o RH pode corrigir
     * um passo, e todo passo fica em historico_status.
     */
    public enum Status { inscrito, em_triagem, entrevista, aprovado, reprovado, contratado, cancelado }
}
