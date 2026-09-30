package com.rh.recrutamento.backend.curriculo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Metadados do PDF do curriculo (item 2.6); o binario fica em disco. */
@Entity
@Table(name = "curriculo_arquivo")
@Getter
public class CurriculoArquivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curriculo_id", nullable = false, unique = true)
    private Curriculo curriculo;

    @Column(name = "nome_original", nullable = false)
    private String nomeOriginal;

    /** Nome gerado (UUID) dentro de app.upload.dir; nunca vem do cliente. */
    @Column(name = "nome_armazenado", nullable = false)
    private String nomeArmazenado;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "tamanho_bytes", nullable = false)
    private Long tamanhoBytes;

    @CreationTimestamp
    @Column(name = "enviado_em", nullable = false)
    private LocalDateTime enviadoEm;

    protected CurriculoArquivo() {
    }

    public CurriculoArquivo(String nomeOriginal, String nomeArmazenado, String contentType, Long tamanhoBytes) {
        this.nomeOriginal = nomeOriginal;
        this.nomeArmazenado = nomeArmazenado;
        this.contentType = contentType;
        this.tamanhoBytes = tamanhoBytes;
    }

    void vincular(Curriculo curriculo) {
        this.curriculo = curriculo;
    }

    /** Reaproveita a linha existente (uk_arquivo_curriculo permite uma por curriculo) em vez de inserir outra. */
    void substituirPor(CurriculoArquivo novo) {
        this.nomeOriginal = novo.nomeOriginal;
        this.nomeArmazenado = novo.nomeArmazenado;
        this.contentType = novo.contentType;
        this.tamanhoBytes = novo.tamanhoBytes;
        this.enviadoEm = LocalDateTime.now();
    }
}
