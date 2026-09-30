package com.rh.recrutamento.backend.service;

import com.rh.recrutamento.backend.dto.auth.UsuarioLogado;
import com.rh.recrutamento.backend.dto.usuario.request.UsuarioRequest;
import com.rh.recrutamento.backend.dto.usuario.request.UsuarioUpdateRequest;
import com.rh.recrutamento.backend.dto.usuario.response.UsuarioResponse;
import com.rh.recrutamento.backend.entity.Usuario;
import com.rh.recrutamento.backend.exception.AcessoNegadoException;
import com.rh.recrutamento.backend.exception.EmailJaCadastradoException;
import com.rh.recrutamento.backend.exception.RecursoNaoEncontradoException;
import com.rh.recrutamento.backend.mapper.UsuarioMapper;
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
    private final UsuarioMapper usuarioMapper;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                           UsuarioMapper usuarioMapper) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioMapper = usuarioMapper;
    }

    /** Sem solicitante (cadastro publico) so cria candidato; RH e administrador exigem um administrador logado. */
    @Transactional
    public UsuarioResponse criar(UsuarioRequest request, UsuarioLogado solicitante) {
        boolean solicitanteEhAdministrador = solicitante != null && solicitante.ehAdministrador();
        if (request.perfil() != Usuario.Perfil.candidato && !solicitanteEhAdministrador) {
            throw new AcessoNegadoException("Somente administradores podem cadastrar usuários do RH.");
        }

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

        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream()
            .map(usuarioMapper::toResponse)
            .toList();
    }

    public UsuarioResponse buscarPorId(Long id, UsuarioLogado logado) {
        if (!logado.ehAdministrador() && !logado.id().equals(id)) {
            throw new AcessoNegadoException("Você só pode consultar os seus próprios dados.");
        }
        return usuarioMapper.toResponse(obterUsuario(id));
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioUpdateRequest request, UsuarioLogado logado) {
        Usuario usuario = obterUsuario(id);
        // impede o administrador de se rebaixar ou se bloquear e deixar o sistema sem ninguem para gerir usuarios
        if (logado.id().equals(id) && (request.perfil() != usuario.getPerfil() || request.status() != usuario.getStatus())) {
            throw new AcessoNegadoException("Você não pode alterar o seu próprio perfil ou status.");
        }

        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmailAndIdNot(email, id)) {
            throw new EmailJaCadastradoException(email);
        }

        usuario.atualizarDados(request.nome().trim(), email, request.perfil(), request.status());

        if (request.senha() != null && !request.senha().isBlank()) {
            usuario.alterarSenha(passwordEncoder.encode(request.senha()));
        }

        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public void excluir(Long id, UsuarioLogado logado) {
        if (logado.id().equals(id)) {
            throw new AcessoNegadoException("Você não pode excluir a sua própria conta.");
        }
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
