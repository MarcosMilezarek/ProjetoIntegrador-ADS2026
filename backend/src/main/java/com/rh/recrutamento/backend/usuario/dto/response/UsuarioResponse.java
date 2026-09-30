package com.rh.recrutamento.backend.usuario.dto.response;

/** Saída de usuário. Nunca expõe o hash da senha. */
public record UsuarioResponse(
    Long id,
    String nome,
    String email,
    String perfil,
    String status
) {}
