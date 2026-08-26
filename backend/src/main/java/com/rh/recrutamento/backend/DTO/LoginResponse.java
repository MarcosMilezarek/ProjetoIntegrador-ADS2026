package com.rh.recrutamento.backend.dto;

public record LoginResponse(
    Long id,
    String nome,
    String email,
    String perfil
) {}