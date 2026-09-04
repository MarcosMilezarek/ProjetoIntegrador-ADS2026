package com.rh.recrutamento.backend.dto;

import com.rh.recrutamento.backend.entity.Usuario;

/** Saída de usuário. Nunca expõe o hash da senha. */
public record UsuarioResponse(
    Long id,
    String nome,
    String email,
    String perfil,
    String status
) {
    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.getPerfil().name(),
            usuario.getStatus().name()
        );
    }
}
