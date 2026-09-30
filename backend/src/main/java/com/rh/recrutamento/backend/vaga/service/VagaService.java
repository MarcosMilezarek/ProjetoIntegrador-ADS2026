package com.rh.recrutamento.backend.vaga.service;

import com.rh.recrutamento.backend.auth.dto.UsuarioLogado;
import com.rh.recrutamento.backend.comum.exception.AcessoNegadoException;
import com.rh.recrutamento.backend.comum.exception.RecursoNaoEncontradoException;
import com.rh.recrutamento.backend.usuario.entity.Usuario;
import com.rh.recrutamento.backend.usuario.repository.UsuarioRepository;
import com.rh.recrutamento.backend.vaga.dto.request.VagaRequest;
import com.rh.recrutamento.backend.vaga.dto.request.VagaUpdateRequest;
import com.rh.recrutamento.backend.vaga.dto.response.VagaResponse;
import com.rh.recrutamento.backend.vaga.entity.Vaga;
import com.rh.recrutamento.backend.vaga.exception.RhInvalidoException;
import com.rh.recrutamento.backend.vaga.mapper.VagaMapper;
import com.rh.recrutamento.backend.vaga.repository.VagaRepository;
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
    public VagaResponse criar(VagaRequest request, UsuarioLogado logado) {
        Usuario rh = obterRh(logado.id());

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

    /** Candidato ve tudo menos rascunho; RH ve as proprias vagas (RN07); administrador ve todas. */
    public List<VagaResponse> listar(UsuarioLogado logado) {
        List<Vaga> vagas = switch (logado.perfil()) {
            case candidato -> vagaRepository.findByStatusNot(Vaga.Status.rascunho);
            case rh -> vagaRepository.findByRh_Id(logado.id());
            case administrador -> vagaRepository.findAll();
        };
        return vagas.stream()
            .map(vagaMapper::toResponse)
            .toList();
    }

    public VagaResponse buscarPorId(Long id, UsuarioLogado logado) {
        Vaga vaga = obterVaga(id);
        if (logado.ehCandidato()) {
            // rascunho e interno do RH: para o candidato, e como se nao existisse
            if (vaga.getStatus() == Vaga.Status.rascunho) {
                throw new RecursoNaoEncontradoException("Vaga " + id + " não encontrada.");
            }
        } else {
            verificarResponsavel(vaga, logado);
        }
        return vagaMapper.toResponse(vaga);
    }

    @Transactional
    public VagaResponse atualizar(Long id, VagaUpdateRequest request, UsuarioLogado logado) {
        Vaga vaga = obterVaga(id);
        verificarResponsavel(vaga, logado);

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

    private void verificarResponsavel(Vaga vaga, UsuarioLogado logado) {
        if (!logado.gerencia(vaga)) {
            throw new AcessoNegadoException("Esta vaga está sob responsabilidade de outro RH.");
        }
    }

    /** Confere no banco (o token pode ser anterior a uma troca de perfil). */
    private Usuario obterRh(Long rhId) {
        Usuario usuario = usuarioRepository.findById(rhId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário " + rhId + " não encontrado."));
        if (usuario.getPerfil() == Usuario.Perfil.candidato) {
            throw new RhInvalidoException(rhId);
        }
        return usuario;
    }
}
