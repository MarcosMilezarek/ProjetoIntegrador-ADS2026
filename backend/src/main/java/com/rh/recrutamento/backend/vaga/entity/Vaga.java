package com.rh.recrutamento.backend.vaga.entity;

import com.rh.recrutamento.backend.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vaga")
@Getter
public class Vaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rh_id", nullable = false)
    private Usuario rh;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "requisitos_obrigatorios", columnDefinition = "TEXT")
    private String requisitosObrigatorios;

    @Column(name = "requisitos_desejaveis", columnDefinition = "TEXT")
    private String requisitosDesejaveis;

    @Column(name = "requisitos_diferenciais", columnDefinition = "TEXT")
    private String requisitosDiferenciais;

    @Column(length = 150)
    private String local;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Modalidade modalidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contrato", nullable = false)
    private TipoContrato tipoContrato;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private LocalDate prazo;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    protected Vaga() {
        // exigido pelo JPA
    }

    public Vaga(Usuario rh, String titulo, String descricao, String requisitosObrigatorios,
                String requisitosDesejaveis, String requisitosDiferenciais, String local,
                Modalidade modalidade, TipoContrato tipoContrato, Status status, LocalDate prazo) {
        this.rh = rh;
        this.titulo = titulo;
        this.descricao = descricao;
        this.requisitosObrigatorios = requisitosObrigatorios;
        this.requisitosDesejaveis = requisitosDesejaveis;
        this.requisitosDiferenciais = requisitosDiferenciais;
        this.local = local;
        this.modalidade = modalidade;
        this.tipoContrato = tipoContrato;
        this.status = status;
        this.prazo = prazo;
    }

    /** Atualiza os dados da vaga, inclusive o status (usado também para encerrar). */
    public void atualizarDados(String titulo, String descricao, String requisitosObrigatorios,
                                String requisitosDesejaveis, String requisitosDiferenciais, String local,
                                Modalidade modalidade, TipoContrato tipoContrato, Status status, LocalDate prazo) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.requisitosObrigatorios = requisitosObrigatorios;
        this.requisitosDesejaveis = requisitosDesejaveis;
        this.requisitosDiferenciais = requisitosDiferenciais;
        this.local = local;
        this.modalidade = modalidade;
        this.tipoContrato = tipoContrato;
        this.status = status;
        this.prazo = prazo;
    }

    public enum Modalidade { presencial, remoto, hibrido }
    public enum TipoContrato { clt, pj, estagio, temporario }
    public enum Status { rascunho, aberta, encerrada }
}
