package com.rh.recrutamento.backend.auth.dto;

import com.rh.recrutamento.backend.usuario.entity.Usuario;
import com.rh.recrutamento.backend.vaga.entity.Vaga;
import org.springframework.security.oauth2.jwt.Jwt;

/** Quem faz a requisicao, lido do JWT validado. A identidade nunca vem do corpo nem da URL. */
public record UsuarioLogado(Long id, Usuario.Perfil perfil) {

    public static UsuarioLogado de(Jwt jwt) {
        return new UsuarioLogado(Long.valueOf(jwt.getSubject()), Usuario.Perfil.valueOf(jwt.getClaimAsString("perfil")));
    }

    public boolean ehAdministrador() {
        return perfil == Usuario.Perfil.administrador;
    }

    public boolean ehCandidato() {
        return perfil == Usuario.Perfil.candidato;
    }

    /** RN07: o RH gerencia so as vagas sob sua responsabilidade; o administrador, todas. */
    public boolean gerencia(Vaga vaga) {
        return ehAdministrador() || (perfil == Usuario.Perfil.rh && vaga.getRh().getId().equals(id));
    }
}
