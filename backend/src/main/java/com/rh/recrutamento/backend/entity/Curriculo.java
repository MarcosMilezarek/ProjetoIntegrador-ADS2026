package com.rh.recrutamento.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "curriculo")
@Getter
public class Curriculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidato_id", nullable = false, unique = true)
    private Candidato candidato;

    @Column(columnDefinition = "TEXT")
    private String formacao;

    @Column(columnDefinition = "TEXT")
    private String experiencias;

    @Column(columnDefinition = "TEXT")
    private String competencias;

    @Column(columnDefinition = "TEXT")
    private String resumo;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    protected Curriculo() {
    }

    public Curriculo(Candidato candidato, String formacao, String experiencias, String competencias, String resumo) {
        this.candidato = candidato;
        this.formacao = formacao;
        this.experiencias = experiencias;
        this.competencias = competencias;
        this.resumo = resumo;
    }

    public void atualizarDados(String formacao, String experiencias, String competencias, String resumo) {
        this.formacao = formacao;
        this.experiencias = experiencias;
        this.competencias = competencias;
        this.resumo = resumo;
    }
}
