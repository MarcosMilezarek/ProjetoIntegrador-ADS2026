package com.rh.recrutamento.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;

/** Extensao 1:1 de Usuario (perfil candidato); autoprovisionada ao cadastrar o curriculo. */
@Entity
@Table(name = "candidato")
@Getter
public class Candidato {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    protected Candidato() {
    }

    public Candidato(Usuario usuario) {
        this.usuario = usuario;
        this.usuarioId = usuario.getId();
    }
}
