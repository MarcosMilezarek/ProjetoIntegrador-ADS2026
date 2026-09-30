package com.rh.recrutamento.backend.curriculo.entity;

import com.rh.recrutamento.backend.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Curriculo do candidato: dados pessoais, contato e conteudo (RF04/RF05).
 * Aponta direto para a conta de acesso ({@link Usuario}) - nao existe tabela
 * intermediaria de candidato.
 */
@Entity
@Table(name = "curriculo")
@Getter
public class Curriculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Enumerated(EnumType.STRING)
    private Sexo sexo;

    @Column(length = 100)
    private String cidade;

    @Column(length = 2)
    private String uf;

    @Column(name = "numero_contato", length = 20)
    private String numeroContato;

    @Column(name = "perfil_linkedin")
    private String perfilLinkedin;

    @Column(columnDefinition = "TEXT")
    private String competencias;

    /** Certificacoes fora da formacao academica; opcional. */
    @Column(columnDefinition = "TEXT")
    private String certificacoes;

    @Column(columnDefinition = "TEXT")
    private String resumo;

    @OneToMany(mappedBy = "curriculo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CurriculoFormacao> formacoes = new ArrayList<>();

    @OneToMany(mappedBy = "curriculo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CurriculoExperiencia> experiencias = new ArrayList<>();

    @OneToOne(mappedBy = "curriculo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private CurriculoArquivo arquivo;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    protected Curriculo() {
    }

    public Curriculo(Usuario usuario) {
        this.usuario = usuario;
    }

    public void atualizarDados(LocalDate dataNascimento, Sexo sexo, String cidade, String uf,
                                String numeroContato, String perfilLinkedin,
                                String competencias, String certificacoes, String resumo) {
        this.dataNascimento = dataNascimento;
        this.sexo = sexo;
        this.cidade = cidade;
        this.uf = uf;
        this.numeroContato = numeroContato;
        this.perfilLinkedin = perfilLinkedin;
        this.competencias = competencias;
        this.certificacoes = certificacoes;
        this.resumo = resumo;
    }

    /** Troca a lista inteira: o cliente sempre envia o estado final da formacao. */
    public void substituirFormacoes(List<CurriculoFormacao> novas) {
        this.formacoes.clear();
        novas.forEach(this::adicionarFormacao);
    }

    public void substituirExperiencias(List<CurriculoExperiencia> novas) {
        this.experiencias.clear();
        novas.forEach(this::adicionarExperiencia);
    }

    public void adicionarFormacao(CurriculoFormacao formacao) {
        formacao.vincular(this);
        this.formacoes.add(formacao);
    }

    public void adicionarExperiencia(CurriculoExperiencia experiencia) {
        experiencia.vincular(this);
        this.experiencias.add(experiencia);
    }

    public void definirArquivo(CurriculoArquivo novo) {
        if (this.arquivo == null) {
            novo.vincular(this);
            this.arquivo = novo;
        } else {
            this.arquivo.substituirPor(novo);
        }
    }

    public enum Sexo { feminino, masculino, outro, nao_informado }
}
