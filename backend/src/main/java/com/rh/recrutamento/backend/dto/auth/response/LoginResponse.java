package com.rh.recrutamento.backend.dto.auth.response;

public record LoginResponse(
    Long id,
    String nome,
    String email,
    String perfil
) {}