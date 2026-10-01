package com.rh.recrutamento.backend.documento.entity;

import com.rh.recrutamento.backend.candidatura.entity.Candidatura;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Documento de contratacao enviado pelo candidato em uma candidatura aprovada (RF09/RN03).
 * Um por tipo em cada candidatura: reenviar o mesmo tipo substitui o arquivo anterior.
 */
@Entity
@Table(name = "documento",
    uniqueConstraints = @UniqueConstraint(name = "uk_documento_candidatura_tipo", columnNames = {"candidatura_id", "tipo"}))
@Getter
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidatura_id", nullable = false)
    private Candidatura candidatura;

    /** Item da lista fechada. Nulo so em envios antigos cujo texto livre nao correspondeu a nenhum tipo. */
    @Enumerated(EnumType.STRING)
    private Tipo tipo;

    /** Texto livre digitado antes da lista fechada (migration V5). Nulo nos envios novos. */
    @Column(name = "tipo_informado", length = 100)
    private String tipoInformado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Formato formato;

    /** Nome gerado do arquivo dentro de app.upload.dir/documento (nao e URL publica; o download passa pela API). */
    @Column(name = "arquivo_url", nullable = false, length = 500)
    private String arquivoUrl;

    @Column(name = "tamanho_bytes", nullable = false)
    private Long tamanhoBytes;

    /** Data do envio; no reenvio do mesmo tipo passa a ser a do arquivo novo. */
    @Column(name = "data_envio", nullable = false)
    private LocalDateTime dataEnvio;

    protected Documento() {
    }

    public Documento(Candidatura candidatura, Tipo tipo, Formato formato, String arquivoUrl, Long tamanhoBytes) {
        this.candidatura = candidatura;
        this.tipo = tipo;
        this.formato = formato;
        this.arquivoUrl = arquivoUrl;
        this.tamanhoBytes = tamanhoBytes;
        this.dataEnvio = LocalDateTime.now();
    }

    /** Reenvio do mesmo tipo: o registro passa a apontar para o arquivo novo. */
    public void substituirArquivo(Formato formato, String arquivoUrl, Long tamanhoBytes) {
        this.formato = formato;
        this.arquivoUrl = arquivoUrl;
        this.tamanhoBytes = tamanhoBytes;
        this.dataEnvio = LocalDateTime.now();
    }

    /** Nome para exibir: o da lista ou, num envio antigo sem tipo, o texto que foi digitado. */
    public String nomeDoTipo() {
        return tipo != null ? tipo.getNome() : tipoInformado;
    }

    /**
     * Documentos que o RH pede para a contratacao, na ordem em que aparecem para o candidato.
     * Os obrigatorios entram na contagem de enviados sobre o total exigido; o condicional so vale
     * para quem se enquadra na condicao.
     */
    public enum Tipo {
        rg("RG", true, null),
        cpf("CPF", true, null),
        ctps("Carteira de Trabalho (CTPS)", true, null),
        titulo_eleitor("Título de eleitor", true, null),
        comprovante_residencia("Comprovante de residência", true, null),
        comprovante_escolaridade("Comprovante de escolaridade", true, null),
        foto_3x4("Foto 3x4", true, null),
        pis_pasep("PIS ou PASEP", true, null),
        certidao_nascimento_casamento("Certidão de nascimento ou casamento", true, null),
        dados_bancarios("Dados bancários", true, null),
        certificado_reservista("Certificado de reservista", false, "Obrigatório para homens de 18 a 45 anos.");

        private final String nome;
        private final boolean obrigatorio;
        private final String condicao;

        Tipo(String nome, boolean obrigatorio, String condicao) {
            this.nome = nome;
            this.obrigatorio = obrigatorio;
            this.condicao = condicao;
        }

        public String getNome() {
            return nome;
        }

        public boolean isObrigatorio() {
            return obrigatorio;
        }

        public String getCondicao() {
            return condicao;
        }
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
