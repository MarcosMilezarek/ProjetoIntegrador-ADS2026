package com.rh.recrutamento.backend.funcionario.entity;

import com.rh.recrutamento.backend.candidatura.entity.Candidatura;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Candidatura contratada. Nasce ativo no ato da contratacao; inativar nao apaga nada
 * (candidato, vaga e documentos continuam acessiveis pela candidatura).
 */
@Entity
@Table(name = "funcionario",
    uniqueConstraints = @UniqueConstraint(name = "uk_funcionario_candidatura", columnNames = "candidatura_id"))
@Getter
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidatura_id", nullable = false)
    private Candidatura candidatura;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @CreationTimestamp
    @Column(name = "data_contratacao", nullable = false, updatable = false)
    private LocalDateTime dataContratacao;

    protected Funcionario() {
    }

    public Funcionario(Candidatura candidatura) {
        this.candidatura = candidatura;
        this.status = Status.ativo;
    }

    public void inativar() {
        this.status = Status.inativo;
    }

    public enum Status { ativo, inativo }
}
