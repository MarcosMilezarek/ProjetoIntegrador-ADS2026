package com.rh.recrutamento.backend.service;

import com.rh.recrutamento.backend.dto.auth.request.LoginRequest;
import com.rh.recrutamento.backend.dto.auth.response.LoginResponse;
import com.rh.recrutamento.backend.entity.Usuario;
import com.rh.recrutamento.backend.exception.CredenciaisInvalidasException;
import com.rh.recrutamento.backend.exception.UsuarioInativoException;
import com.rh.recrutamento.backend.mapper.UsuarioMapper;
import com.rh.recrutamento.backend.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Autenticação por e-mail e senha (RF01). */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;
    private final TokenService tokenService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                        UsuarioMapper usuarioMapper, TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioMapper = usuarioMapper;
        this.tokenService = tokenService;
    }

    public LoginResponse autenticar(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email().trim().toLowerCase())
            .orElseThrow(CredenciaisInvalidasException::new);

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException();
        }

        if (!usuario.estaAtivo()) {
            throw new UsuarioInativoException();
        }

        return usuarioMapper.toLoginResponse(usuario, tokenService.gerar(usuario));
    }
}
