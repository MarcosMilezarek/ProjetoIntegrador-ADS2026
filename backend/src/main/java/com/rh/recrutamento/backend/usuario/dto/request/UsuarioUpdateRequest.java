package com.rh.recrutamento.backend.usuario.dto.request;

import com.rh.recrutamento.backend.usuario.entity.Usuario;
import com.rh.recrutamento.backend.comum.validation.SenhaCompativel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Entrada para atualização de usuário (PUT /usuarios/{id}). */
public record UsuarioUpdateRequest(

    @NotBlank(message = "O nome é obrigatório.")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
    String nome,

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "O e-mail deve ser válido.")
    @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
    String email,

    @NotNull(message = "O perfil é obrigatório.")
    Usuario.Perfil perfil,

    @NotNull(message = "O status é obrigatório.")
    Usuario.Status status,

    /** Opcional: quando informada, substitui a senha atual. */
    @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres.")
    @SenhaCompativel
    String senha
) {}
