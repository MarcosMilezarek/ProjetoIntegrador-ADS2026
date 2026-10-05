package com.rh.recrutamento.backend.analise.entity;

import com.rh.recrutamento.backend.candidatura.entity.Candidatura;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** Triagem por IA de uma candidatura: uma por candidatura, so o RH a le. */
@Entity
@Table(name = "analise_candidatura")
@Getter
public class AnaliseCandidatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidatura_id", nullable = false, unique = true)
    private Candidatura candidatura;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    /** Aderencia do candidato a vaga, de 0 a 100 (%). Nula enquanto nao concluida. */
    private Integer aderencia;

    /** Lista JSON de textos; nula enquanto nao concluida. */
    @Column(name = "pontos_positivos", columnDefinition = "TEXT")
    private String pontosPositivos;

    /** Lista JSON de textos; nula enquanto nao concluida. */
    @Column(name = "pontos_negativos", columnDefinition = "TEXT")
    private String pontosNegativos;

    private String modelo;

    /** Criacao e ultima mudanca de status. */
    @UpdateTimestamp
    @Column(name = "data_analise", nullable = false)
    private LocalDateTime dataAnalise;

    protected AnaliseCandidatura() {
    }

    public AnaliseCandidatura(Candidatura candidatura) {
        this.candidatura = candidatura;
        this.status = Status.PENDENTE;
    }

    public void concluir(int aderencia, String pontosPositivos, String pontosNegativos, String modelo) {
        this.status = Status.CONCLUIDA;
        this.aderencia = aderencia;
        this.pontosPositivos = pontosPositivos;
        this.pontosNegativos = pontosNegativos;
        this.modelo = modelo;
    }

    public void falhar(String modelo) {
        this.status = Status.FALHA;
        this.modelo = modelo;
    }

    public enum Status { PENDENTE, CONCLUIDA, FALHA }
}
