package com.rh.recrutamento.backend.curriculo.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;

/** Experiencia profissional do curriculo (item 2.1). */
@Entity
@Table(name = "curriculo_experiencia")
@Getter
public class CurriculoExperiencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curriculo_id", nullable = false)
    private Curriculo curriculo;

    @Column(nullable = false, length = 150)
    private String cargo;

    @Column(nullable = false, length = 150)
    private String empresa;

    @Column(name = "data_contratacao", nullable = false)
    private LocalDate dataContratacao;

    @Column(name = "data_demissao")
    private LocalDate dataDemissao;

    @Column(name = "trabalho_atual", nullable = false)
    private boolean trabalhoAtual;

    @Column(name = "descricao_atividades", columnDefinition = "TEXT")
    private String descricaoAtividades;

    protected CurriculoExperiencia() {
    }

    public CurriculoExperiencia(String cargo, String empresa, LocalDate dataContratacao,
                                 LocalDate dataDemissao, boolean trabalhoAtual, String descricaoAtividades) {
        this.cargo = cargo;
        this.empresa = empresa;
        this.dataContratacao = dataContratacao;
        this.trabalhoAtual = trabalhoAtual;
        // emprego atual nao tem data de demissao: a data enviada e descartada
        this.dataDemissao = trabalhoAtual ? null : dataDemissao;
        this.descricaoAtividades = descricaoAtividades;
    }

    void vincular(Curriculo curriculo) {
        this.curriculo = curriculo;
    }
}
