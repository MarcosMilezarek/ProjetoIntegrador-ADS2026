package com.rh.recrutamento.backend.dto.auth.response;

public record LoginResponse(
    Long id,
    String nome,
    String email,
    String perfil,
    /** JWT a enviar em "Authorization: Bearer <token>" nas demais rotas. Vale 8 horas. */
    String token
) {}