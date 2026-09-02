package com.rh.recrutamento.backend.service;
import com.rh.recrutamento.backend.dto.LoginRequest;
import com.rh.recrutamento.backend.dto.LoginResponse;
import com.rh.recrutamento.backend.entity.Usuario;
import com.rh.recrutamento.backend.repository.UsuarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public LoginResponse autenticar(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
            .orElseThrow(this::credenciaisInvalidas);

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw credenciaisInvalidas();
        }

        if (usuario.getStatus() != Usuario.Status.ativo) {
            throw new IllegalStateException("Usuário bloqueado ou inativo.");
        }

        return new LoginResponse(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.getPerfil().name()
        );
    }

    private IllegalArgumentException credenciaisInvalidas() {
        // Mensagem genérica de propósito: não revela se o erro foi no e-mail ou na senha (RNF02/segurança)
        return new IllegalArgumentException("E-mail ou senha inválidos.");
    }
}