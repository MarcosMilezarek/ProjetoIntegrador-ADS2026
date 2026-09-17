package com.rh.recrutamento.backend.service;

import com.rh.recrutamento.backend.dto.vaga.request.VagaRequest;
import com.rh.recrutamento.backend.dto.vaga.request.VagaUpdateRequest;
import com.rh.recrutamento.backend.dto.vaga.response.VagaResponse;
import com.rh.recrutamento.backend.entity.Usuario;
import com.rh.recrutamento.backend.entity.Vaga;
import com.rh.recrutamento.backend.exception.RecursoNaoEncontradoException;
import com.rh.recrutamento.backend.exception.RhInvalidoException;
import com.rh.recrutamento.backend.mapper.VagaMapper;
import com.rh.recrutamento.backend.repository.UsuarioRepository;
import com.rh.recrutamento.backend.repository.VagaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Regras de negócio do cadastro de vagas (RF10/RF11, RN02). */
@Service
@Transactional(readOnly = true)
public class VagaService {

    private final VagaRepository vagaRepository;
    private final UsuarioRepository usuarioRepository;
    private final VagaMapper vagaMapper;

    public VagaService(VagaRepository vagaRepository, UsuarioRepository usuarioRepository, VagaMapper vagaMapper) {
        this.vagaRepository = vagaRepository;
        this.usuarioRepository = usuarioRepository;
        this.vagaMapper = vagaMapper;
    }

    @Transactional
    public VagaResponse criar(VagaRequest request) {
        Usuario rh = obterRh(request.rhId());

        Vaga vaga = new Vaga(
            rh,
            request.titulo().trim(),
            request.descricao().trim(),
            request.requisitos(),
            request.local(),
            request.modalidade(),
            request.tipoContrato(),
            request.status() != null ? request.status() : Vaga.Status.rascunho,
            request.prazo()
        );

        return vagaMapper.toResponse(vagaRepository.save(vaga));
    }

    public List<VagaResponse> listar() {
        return vagaRepository.findAll().stream()
            .map(vagaMapper::toResponse)
            .toList();
    }

    public VagaResponse buscarPorId(Long id) {
        return vagaMapper.toResponse(obterVaga(id));
    }

    @Transactional
    public VagaResponse atualizar(Long id, VagaUpdateRequest request) {
        Vaga vaga = obterVaga(id);

        vaga.atualizarDados(
            request.titulo().trim(),
            request.descricao().trim(),
            request.requisitos(),
            request.local(),
            request.modalidade(),
            request.tipoContrato(),
            request.status(),
            request.prazo()
        );

        return vagaMapper.toResponse(vagaRepository.save(vaga));
    }

    private Vaga obterVaga(Long id) {
        return vagaRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Vaga " + id + " não encontrada."));
    }

    private Usuario obterRh(Long rhId) {
        Usuario usuario = usuarioRepository.findById(rhId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário " + rhId + " não encontrado."));
        if (usuario.getPerfil() != Usuario.Perfil.rh) {
            throw new RhInvalidoException(rhId);
        }
        return usuario;
    }
}
