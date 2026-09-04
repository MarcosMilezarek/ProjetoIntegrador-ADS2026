package com.rh.recrutamento.backend.dto;

import com.rh.recrutamento.backend.entity.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Entrada para criação de usuário (POST /usuarios). */
public record UsuarioRequest(

    @NotBlank(message = "O nome é obrigatório.")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
    String nome,

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "O e-mail deve ser válido.")
    @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
    String email,

    @NotBlank(message = "A senha é obrigatória.")
    @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres.")
    String senha,

    @NotNull(message = "O perfil é obrigatório.")
    Usuario.Perfil perfil,

    /** Opcional: quando ausente o usuário é criado como ativo. */
    Usuario.Status status
) {}
