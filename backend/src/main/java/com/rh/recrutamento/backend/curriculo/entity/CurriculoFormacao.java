package com.rh.recrutamento.backend.curriculo.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;

/** Formacao academica do curriculo (item 2.2). */
@Entity
@Table(name = "curriculo_formacao")
@Getter
public class CurriculoFormacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curriculo_id", nullable = false)
    private Curriculo curriculo;

    @Column(nullable = false, length = 150)
    private String curso;

    @Column(nullable = false, length = 150)
    private String instituicao;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    /** Nula enquanto o curso esta em andamento. */
    @Column(name = "data_termino")
    private LocalDate dataTermino;

    protected CurriculoFormacao() {
    }

    public CurriculoFormacao(String curso, String instituicao, LocalDate dataInicio, LocalDate dataTermino) {
        this.curso = curso;
        this.instituicao = instituicao;
        this.dataInicio = dataInicio;
        this.dataTermino = dataTermino;
    }

    void vincular(Curriculo curriculo) {
        this.curriculo = curriculo;
    }
}
