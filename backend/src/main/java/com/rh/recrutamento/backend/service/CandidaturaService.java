package com.rh.recrutamento.backend.service;

import com.rh.recrutamento.backend.dto.auth.UsuarioLogado;
import com.rh.recrutamento.backend.dto.candidatura.request.CandidaturaRequest;
import com.rh.recrutamento.backend.dto.candidatura.request.StatusCandidaturaRequest;
import com.rh.recrutamento.backend.dto.candidatura.response.CandidaturaResponse;
import com.rh.recrutamento.backend.entity.Candidatura;
import com.rh.recrutamento.backend.entity.HistoricoStatus;
import com.rh.recrutamento.backend.entity.Usuario;
import com.rh.recrutamento.backend.entity.Vaga;
import com.rh.recrutamento.backend.exception.AcessoNegadoException;
import com.rh.recrutamento.backend.exception.CandidaturaNaoPermitidaException;
import com.rh.recrutamento.backend.exception.RecursoNaoEncontradoException;
import com.rh.recrutamento.backend.mapper.CandidaturaMapper;
import com.rh.recrutamento.backend.repository.CandidaturaRepository;
import com.rh.recrutamento.backend.repository.CurriculoRepository;
import com.rh.recrutamento.backend.repository.HistoricoStatusRepository;
import com.rh.recrutamento.backend.repository.UsuarioRepository;
import com.rh.recrutamento.backend.repository.VagaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Candidatura (RF07/RF08) e painel do RH: inscritos por vaga e etapa do processo (RF12/RF13). */
@Service
@Transactional(readOnly = true)
public class CandidaturaService {

    private final CandidaturaRepository candidaturaRepository;
    private final HistoricoStatusRepository historicoRepository;
    private final VagaRepository vagaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CurriculoRepository curriculoRepository;
    private final CandidaturaMapper candidaturaMapper;

    public CandidaturaService(CandidaturaRepository candidaturaRepository, HistoricoStatusRepository historicoRepository,
                              VagaRepository vagaRepository, UsuarioRepository usuarioRepository,
                              CurriculoRepository curriculoRepository, CandidaturaMapper candidaturaMapper) {
        this.candidaturaRepository = candidaturaRepository;
        this.historicoRepository = historicoRepository;
        this.vagaRepository = vagaRepository;
        this.usuarioRepository = usuarioRepository;
        this.curriculoRepository = curriculoRepository;
        this.candidaturaMapper = candidaturaMapper;
    }

    @Transactional
    public CandidaturaResponse candidatar(CandidaturaRequest request, UsuarioLogado logado) {
        Vaga vaga = obterVaga(request.vagaId());
        if (vaga.getStatus() != Vaga.Status.aberta) {
            throw new CandidaturaNaoPermitidaException("Esta vaga não está aberta para candidaturas.");
        }
        if (!curriculoRepository.existsByUsuario_Id(logado.id())) {
            throw new CandidaturaNaoPermitidaException("Cadastre seu currículo antes de se candidatar.");
        }
        if (candidaturaRepository.existsByCandidato_IdAndVaga_Id(logado.id(), vaga.getId())) {
            throw new CandidaturaNaoPermitidaException("Você já se candidatou a esta vaga.");
        }

        Usuario candidato = usuarioRepository.getReferenceById(logado.id());
        Candidatura candidatura = candidaturaRepository.save(new Candidatura(candidato, vaga));
        historicoRepository.save(new HistoricoStatus(
            candidatura, null, Candidatura.Status.inscrito, candidato, "Candidatura registrada pelo portal."));
        return candidaturaMapper.toResponse(candidatura);
    }

    /** RN06: o candidato ve so as proprias candidaturas. */
    public List<CandidaturaResponse> listarMinhas(UsuarioLogado logado) {
        return candidaturaRepository.findByCandidato_IdOrderByDataCandidaturaDesc(logado.id()).stream()
            .map(candidaturaMapper::toResponse)
            .toList();
    }

    public List<CandidaturaResponse> listarPorVaga(Long vagaId, UsuarioLogado logado) {
        verificarResponsavel(obterVaga(vagaId), logado);
        return candidaturaRepository.findByVaga_IdOrderByDataCandidaturaAsc(vagaId).stream()
            .map(candidaturaMapper::toResponse)
            .toList();
    }

    @Transactional
    public CandidaturaResponse alterarStatus(Long id, StatusCandidaturaRequest request, UsuarioLogado logado) {
        Candidatura candidatura = candidaturaRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Candidatura " + id + " não encontrada."));
        verificarResponsavel(candidatura.getVaga(), logado);

        Candidatura.Status anterior = candidatura.getStatus();
        if (anterior != request.status()) {
            candidatura.alterarStatus(request.status());
            historicoRepository.save(new HistoricoStatus(candidatura, anterior, request.status(),
                usuarioRepository.getReferenceById(logado.id()), request.observacao()));
        }
        return candidaturaMapper.toResponse(candidatura);
    }

    private void verificarResponsavel(Vaga vaga, UsuarioLogado logado) {
        if (!logado.gerencia(vaga)) {
            throw new AcessoNegadoException("Esta vaga está sob responsabilidade de outro RH.");
        }
    }

    private Vaga obterVaga(Long id) {
        return vagaRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Vaga " + id + " não encontrada."));
    }
}
