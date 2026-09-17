package com.rh.recrutamento.backend.dto.usuario.response;

/** Saída de usuário. Nunca expõe o hash da senha. */
public record UsuarioResponse(
    Long id,
    String nome,
    String email,
    String perfil,
    String status
) {}
