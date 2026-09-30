package com.rh.recrutamento.backend.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.rh.recrutamento.backend.dto.comum.ErroResponse;
import com.rh.recrutamento.backend.entity.Usuario;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Autenticacao stateless por JWT (HS256). O token carrega o id do usuario (sub) e o
 * perfil (claim "perfil"), que vira o papel ROLE_candidato, ROLE_rh ou ROLE_administrador.
 * Aqui fica a regra por papel; a regra de propriedade (cada um so ve o que e seu) fica nos services.
 */
@Configuration
public class SegurancaConfig {

    private static final String CANDIDATO = Usuario.Perfil.candidato.name();
    private static final String RH = Usuario.Perfil.rh.name();
    private static final String ADMINISTRADOR = Usuario.Perfil.administrador.name();

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Sem valor padrao de proposito: sem JWT_SECRET a aplicacao nao sobe, e nenhum token e assinado com segredo publico. */
    @Bean
    public SecretKey chaveJwt(@Value("${app.jwt.secret}") String segredo) {
        if (segredo.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret (JWT_SECRET) deve ter pelo menos 32 caracteres.");
        }
        return new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey chaveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey chaveJwt) {
        return NimbusJwtDecoder.withSecretKey(chaveJwt).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public SecurityFilterChain filtroSeguranca(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        AuthenticationEntryPoint naoAutenticado = (requisicao, resposta, erro) -> responderErro(
            resposta, objectMapper, HttpStatus.UNAUTHORIZED, "Sessão inválida ou expirada. Faça login novamente.");
        AccessDeniedHandler semPermissao = (requisicao, resposta, erro) -> responderErro(
            resposta, objectMapper, HttpStatus.FORBIDDEN, "Seu perfil não tem permissão para esta ação.");

        http
            // API sem cookie de sessao: o token vai no cabecalho Authorization, entao CSRF nao se aplica
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(rotas -> rotas
                .requestMatchers("/error").permitAll()
                // login e cadastro publico (o service so aceita perfil candidato sem token de administrador)
                .requestMatchers(HttpMethod.POST, "/auth/login", "/usuarios").permitAll()

                // Configuracoes: gestao de usuarios so pelo administrador
                .requestMatchers(HttpMethod.GET, "/usuarios").hasRole(ADMINISTRADOR)
                .requestMatchers(HttpMethod.PUT, "/usuarios/*").hasRole(ADMINISTRADOR)
                .requestMatchers(HttpMethod.DELETE, "/usuarios/*").hasRole(ADMINISTRADOR)

                // vagas: escrita so pelo RH (RN02); o administrador tem os mesmos poderes do RH
                .requestMatchers(HttpMethod.POST, "/vagas").hasAnyRole(RH, ADMINISTRADOR)
                .requestMatchers(HttpMethod.PUT, "/vagas/*").hasAnyRole(RH, ADMINISTRADOR)

                // candidaturas: o candidato se inscreve e acompanha as proprias; o RH gerencia as das suas vagas
                .requestMatchers(HttpMethod.POST, "/candidaturas").hasRole(CANDIDATO)
                .requestMatchers(HttpMethod.GET, "/candidaturas/minhas").hasRole(CANDIDATO)
                .requestMatchers(HttpMethod.GET, "/vagas/*/candidaturas").hasAnyRole(RH, ADMINISTRADOR)
                .requestMatchers(HttpMethod.PUT, "/candidaturas/*/status").hasAnyRole(RH, ADMINISTRADOR)

                // curriculo: so o proprio candidato escreve
                .requestMatchers(HttpMethod.POST, "/curriculos", "/curriculos/*/arquivo").hasRole(CANDIDATO)
                .requestMatchers(HttpMethod.PUT, "/curriculos/*").hasRole(CANDIDATO)

                .anyRequest().authenticated())
            .oauth2ResourceServer(servidor -> servidor
                .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDePapeis()))
                .authenticationEntryPoint(naoAutenticado)
                .accessDeniedHandler(semPermissao))
            .exceptionHandling(erros -> erros
                .authenticationEntryPoint(naoAutenticado)
                .accessDeniedHandler(semPermissao));

        return http.build();
    }

    /** Converte a claim "perfil" no papel ROLE_&lt;perfil&gt; usado em hasRole. */
    private JwtAuthenticationConverter conversorDePapeis() {
        JwtGrantedAuthoritiesConverter papeis = new JwtGrantedAuthoritiesConverter();
        papeis.setAuthoritiesClaimName("perfil");
        papeis.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(papeis);
        return conversor;
    }

    /** Mesmo formato de erro do GlobalExceptionHandler, para o frontend tratar tudo igual. */
    private static void responderErro(HttpServletResponse resposta, ObjectMapper objectMapper,
                                      HttpStatus status, String mensagem) throws IOException {
        resposta.setStatus(status.value());
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(resposta.getOutputStream(),
            ErroResponse.de(status.value(), status.getReasonPhrase(), mensagem));
    }
}
