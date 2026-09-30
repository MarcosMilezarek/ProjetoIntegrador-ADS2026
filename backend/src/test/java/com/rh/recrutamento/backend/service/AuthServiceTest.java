package com.rh.recrutamento.backend.service;

import com.rh.recrutamento.backend.dto.auth.request.LoginRequest;
import com.rh.recrutamento.backend.dto.auth.response.LoginResponse;
import com.rh.recrutamento.backend.entity.Usuario;
import com.rh.recrutamento.backend.exception.CredenciaisInvalidasException;
import com.rh.recrutamento.backend.exception.UsuarioInativoException;
import com.rh.recrutamento.backend.mapper.UsuarioMapperImpl;
import com.rh.recrutamento.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    private AuthService authService;

    @BeforeEach
    void montarService() {
        authService = new AuthService(usuarioRepository, passwordEncoder, new UsuarioMapperImpl(), tokenService);
    }

    @Test
    void deveAutenticarUsuarioAtivo() {
        when(usuarioRepository.findByEmail("marina@email.com"))
            .thenReturn(Optional.of(usuario(Usuario.Status.ativo)));
        when(passwordEncoder.matches("senha123", "hash")).thenReturn(true);
        when(tokenService.gerar(any(Usuario.class))).thenReturn("jwt-gerado");

        LoginResponse resposta = authService.autenticar(new LoginRequest(" Marina@Email.com ", "senha123"));

        assertThat(resposta.email()).isEqualTo("marina@email.com");
        assertThat(resposta.perfil()).isEqualTo("candidato");
        assertThat(resposta.token()).isEqualTo("jwt-gerado");
    }

    @Test
    void deveRecusarEmailInexistente() {
        when(usuarioRepository.findByEmail("ninguem@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.autenticar(new LoginRequest("ninguem@email.com", "senha123")))
            .isInstanceOf(CredenciaisInvalidasException.class)
            .hasMessage("E-mail ou senha inválidos.");
    }

    @Test
    void deveRecusarSenhaIncorreta() {
        when(usuarioRepository.findByEmail("marina@email.com"))
            .thenReturn(Optional.of(usuario(Usuario.Status.ativo)));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.autenticar(new LoginRequest("marina@email.com", "errada")))
            .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    void deveBloquearUsuarioInativo() {
        when(usuarioRepository.findByEmail("marina@email.com"))
            .thenReturn(Optional.of(usuario(Usuario.Status.bloqueado)));
        when(passwordEncoder.matches("senha123", "hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.autenticar(new LoginRequest("marina@email.com", "senha123")))
            .isInstanceOf(UsuarioInativoException.class);
        verifyNoInteractions(tokenService);
    }

    private Usuario usuario(Usuario.Status status) {
        return new Usuario("Marina", "marina@email.com", "hash", Usuario.Perfil.candidato, status);
    }
}
