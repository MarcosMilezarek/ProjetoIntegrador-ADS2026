package com.rh.recrutamento.backend.usuario.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "usuario", uniqueConstraints = @UniqueConstraint(name = "uk_usuario_email", columnNames = "email"))
@Getter
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Perfil perfil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    protected Usuario() {
        // exigido pelo JPA
    }

    public Usuario(String nome, String email, String senhaHash, Perfil perfil, Status status) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.status = status;
    }

    /** Atualiza os dados cadastrais. A senha tem fluxo próprio em {@link #alterarSenha(String)}. */
    public void atualizarDados(String nome, String email, Perfil perfil, Status status) {
        this.nome = nome;
        this.email = email;
        this.perfil = perfil;
        this.status = status;
    }

    public void alterarSenha(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public boolean estaAtivo() {
        return this.status == Status.ativo;
    }

    public enum Perfil { candidato, rh, administrador }
    public enum Status { ativo, inativo, bloqueado }
}
