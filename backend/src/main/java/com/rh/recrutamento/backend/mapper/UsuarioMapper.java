package com.rh.recrutamento.backend.mapper;

import com.rh.recrutamento.backend.dto.auth.response.LoginResponse;
import com.rh.recrutamento.backend.dto.usuario.response.UsuarioResponse;
import com.rh.recrutamento.backend.entity.Usuario;
import org.mapstruct.Mapper;

/** Conversão entre {@link Usuario} e seus DTOs de saída. */
@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    UsuarioResponse toResponse(Usuario usuario);

    LoginResponse toLoginResponse(Usuario usuario, String token);
}
