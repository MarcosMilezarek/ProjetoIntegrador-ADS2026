package com.rh.recrutamento.backend.documento.entity;

import com.rh.recrutamento.backend.candidatura.entity.Candidatura;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Documento de contratacao enviado pelo candidato em uma candidatura aprovada (RF09/RN03). */
@Entity
@Table(name = "documento")
@Getter
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidatura_id", nullable = false)
    private Candidatura candidatura;

    /** Ex.: RG, CPF, comprovante de residencia. */
    @Column(nullable = false, length = 100)
    private String tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Formato formato;

    /** Nome gerado do arquivo dentro de app.upload.dir/documento (nao e URL publica; o download passa pela API). */
    @Column(name = "arquivo_url", nullable = false, length = 500)
    private String arquivoUrl;

    @Column(name = "tamanho_bytes", nullable = false)
    private Long tamanhoBytes;

    @CreationTimestamp
    @Column(name = "data_envio", nullable = false, updatable = false)
    private LocalDateTime dataEnvio;

    protected Documento() {
    }

    public Documento(Candidatura candidatura, String tipo, Formato formato, String arquivoUrl, Long tamanhoBytes) {
        this.candidatura = candidatura;
        this.tipo = tipo;
        this.formato = formato;
        this.arquivoUrl = arquivoUrl;
        this.tamanhoBytes = tamanhoBytes;
    }

    /** Formatos aceitos (RN08). */
    public enum Formato {
        pdf("application/pdf"),
        docx("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        private final String contentType;

        Formato(String contentType) {
            this.contentType = contentType;
        }

        public String getContentType() {
            return contentType;
        }
    }
}
