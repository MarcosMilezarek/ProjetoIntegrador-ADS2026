package com.rh.recrutamento.backend;

import com.rh.recrutamento.backend.usuario.entity.Usuario;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/** Atalhos de teste: requisicao com um JWT ja validado para o perfil, sem passar pelo login. */
public final class Autenticacao {

    private Autenticacao() {
    }

    public static RequestPostProcessor comoCandidato(long id) {
        return como(id, Usuario.Perfil.candidato);
    }

    public static RequestPostProcessor comoRh(long id) {
        return como(id, Usuario.Perfil.rh);
    }

    public static RequestPostProcessor comoAdministrador(long id) {
        return como(id, Usuario.Perfil.administrador);
    }

    private static RequestPostProcessor como(long id, Usuario.Perfil perfil) {
        return jwt()
            .jwt(token -> token.subject(String.valueOf(id)).claim("perfil", perfil.name()))
            .authorities(new SimpleGrantedAuthority("ROLE_" + perfil.name()));
    }
}
