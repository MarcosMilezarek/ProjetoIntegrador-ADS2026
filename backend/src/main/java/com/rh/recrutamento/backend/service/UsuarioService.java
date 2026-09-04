package com.rh.recrutamento.backend.service;

import com.rh.recrutamento.backend.dto.UsuarioRequest;
import com.rh.recrutamento.backend.dto.UsuarioResponse;
import com.rh.recrutamento.backend.dto.UsuarioUpdateRequest;
import com.rh.recrutamento.backend.entity.Usuario;
import com.rh.recrutamento.backend.exception.EmailJaCadastradoException;
import com.rh.recrutamento.backend.exception.RecursoNaoEncontradoException;
import com.rh.recrutamento.backend.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Regras de negócio do cadastro de usuários (RF01/RF02). */
@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponse criar(UsuarioRequest request) {
        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new EmailJaCadastradoException(email);
        }

        Usuario usuario = new Usuario(
            request.nome().trim(),
            email,
            passwordEncoder.encode(request.senha()),
            request.perfil(),
            request.status() != null ? request.status() : Usuario.Status.ativo
        );

        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }

    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream()
            .map(UsuarioResponse::de)
            .toList();
    }

    public UsuarioResponse buscarPorId(Long id) {
        return UsuarioResponse.de(obterUsuario(id));
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = obterUsuario(id);

        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmailAndIdNot(email, id)) {
            throw new EmailJaCadastradoException(email);
        }

        usuario.atualizarDados(request.nome().trim(), email, request.perfil(), request.status());

        if (request.senha() != null && !request.senha().isBlank()) {
            usuario.alterarSenha(passwordEncoder.encode(request.senha()));
        }

        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }

    @Transactional
    public void excluir(Long id) {
        usuarioRepository.delete(obterUsuario(id));
    }

    private Usuario obterUsuario(Long id) {
        return usuarioRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário " + id + " não encontrado."));
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }
}
