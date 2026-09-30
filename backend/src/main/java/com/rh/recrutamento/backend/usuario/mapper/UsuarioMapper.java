package com.rh.recrutamento.backend.usuario.mapper;

import com.rh.recrutamento.backend.auth.dto.response.LoginResponse;
import com.rh.recrutamento.backend.usuario.dto.response.UsuarioResponse;
import com.rh.recrutamento.backend.usuario.entity.Usuario;
import org.mapstruct.Mapper;

/** Conversão entre {@link Usuario} e seus DTOs de saída. */
@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    UsuarioResponse toResponse(Usuario usuario);

    LoginResponse toLoginResponse(Usuario usuario, String token);
}
