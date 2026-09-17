package com.rh.recrutamento.backend.service;

import com.rh.recrutamento.backend.dto.curriculo.request.CurriculoRequest;
import com.rh.recrutamento.backend.dto.curriculo.request.CurriculoUpdateRequest;
import com.rh.recrutamento.backend.dto.curriculo.response.CurriculoResponse;
import com.rh.recrutamento.backend.entity.Candidato;
import com.rh.recrutamento.backend.entity.Curriculo;
import com.rh.recrutamento.backend.entity.Usuario;
import com.rh.recrutamento.backend.exception.CandidatoInvalidoException;
import com.rh.recrutamento.backend.exception.CurriculoJaExisteException;
import com.rh.recrutamento.backend.exception.RecursoNaoEncontradoException;
import com.rh.recrutamento.backend.mapper.CurriculoMapper;
import com.rh.recrutamento.backend.repository.CandidatoRepository;
import com.rh.recrutamento.backend.repository.CurriculoRepository;
import com.rh.recrutamento.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Regras de negocio do cadastro de curriculo (RF04/RF05). */
@Service
@Transactional(readOnly = true)
public class CurriculoService {

    private final CurriculoRepository curriculoRepository;
    private final CandidatoRepository candidatoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CurriculoMapper curriculoMapper;

    public CurriculoService(CurriculoRepository curriculoRepository, CandidatoRepository candidatoRepository,
                             UsuarioRepository usuarioRepository, CurriculoMapper curriculoMapper) {
        this.curriculoRepository = curriculoRepository;
        this.candidatoRepository = candidatoRepository;
        this.usuarioRepository = usuarioRepository;
        this.curriculoMapper = curriculoMapper;
    }

    @Transactional
    public CurriculoResponse criar(CurriculoRequest request) {
        Candidato candidato = obterOuCriarCandidato(request.usuarioId());

        if (curriculoRepository.existsByCandidato_UsuarioId(candidato.getUsuarioId())) {
            throw new CurriculoJaExisteException(candidato.getUsuarioId());
        }

        Curriculo curriculo = new Curriculo(
            candidato, request.formacao(), request.experiencias(), request.competencias(), request.resumo());

        return curriculoMapper.toResponse(curriculoRepository.save(curriculo));
    }

    public CurriculoResponse buscarPorId(Long id) {
        return curriculoMapper.toResponse(obterCurriculo(id));
    }

    public CurriculoResponse buscarPorUsuario(Long usuarioId) {
        Curriculo curriculo = curriculoRepository.findByCandidato_UsuarioId(usuarioId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Curriculo do usuario " + usuarioId + " nao encontrado."));
        return curriculoMapper.toResponse(curriculo);
    }

    @Transactional
    public CurriculoResponse atualizar(Long id, CurriculoUpdateRequest request) {
        Curriculo curriculo = obterCurriculo(id);
        curriculo.atualizarDados(request.formacao(), request.experiencias(), request.competencias(), request.resumo());
        return curriculoMapper.toResponse(curriculoRepository.save(curriculo));
    }

    private Curriculo obterCurriculo(Long id) {
        return curriculoRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Curriculo " + id + " nao encontrado."));
    }

    private Candidato obterOuCriarCandidato(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario " + usuarioId + " nao encontrado."));
        if (usuario.getPerfil() != Usuario.Perfil.candidato) {
            throw new CandidatoInvalidoException(usuarioId);
        }
        return candidatoRepository.findById(usuarioId)
            .orElseGet(() -> candidatoRepository.save(new Candidato(usuario)));
    }
}
