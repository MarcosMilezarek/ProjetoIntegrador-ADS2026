package com.rh.recrutamento.backend.auth.service;

import com.rh.recrutamento.backend.usuario.entity.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/** Emite o JWT do login. Mudanca de perfil ou status so vale no proximo login. */
@Service
public class TokenService {

    static final Duration VALIDADE = Duration.ofHours(8);

    private final JwtEncoder jwtEncoder;

    public TokenService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String gerar(Usuario usuario) {
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject(String.valueOf(usuario.getId()))
            .claim("perfil", usuario.getPerfil().name())
            .issuedAt(agora)
            .expiresAt(agora.plus(VALIDADE))
            .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
    }
}
