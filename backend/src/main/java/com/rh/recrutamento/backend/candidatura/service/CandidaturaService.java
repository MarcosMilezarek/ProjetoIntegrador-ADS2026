package com.rh.recrutamento.backend.candidatura.service;

import com.rh.recrutamento.backend.auth.dto.UsuarioLogado;
import com.rh.recrutamento.backend.candidatura.dto.request.CandidaturaRequest;
import com.rh.recrutamento.backend.candidatura.dto.request.EntrevistaRequest;
import com.rh.recrutamento.backend.candidatura.dto.request.StatusCandidaturaRequest;
import com.rh.recrutamento.backend.candidatura.dto.response.CandidaturaResponse;
import com.rh.recrutamento.backend.candidatura.entity.Candidatura;
import com.rh.recrutamento.backend.candidatura.entity.HistoricoStatus;
import com.rh.recrutamento.backend.candidatura.exception.CandidaturaNaoPermitidaException;
import com.rh.recrutamento.backend.candidatura.mapper.CandidaturaMapper;
import com.rh.recrutamento.backend.candidatura.repository.CandidaturaRepository;
import com.rh.recrutamento.backend.candidatura.repository.HistoricoStatusRepository;
import com.rh.recrutamento.backend.comum.exception.AcessoNegadoException;
import com.rh.recrutamento.backend.comum.exception.RecursoNaoEncontradoException;
import com.rh.recrutamento.backend.curriculo.repository.CurriculoRepository;
import com.rh.recrutamento.backend.notificacao.service.NotificacaoService;
import com.rh.recrutamento.backend.usuario.entity.Usuario;
import com.rh.recrutamento.backend.usuario.repository.UsuarioRepository;
import com.rh.recrutamento.backend.vaga.entity.Vaga;
import com.rh.recrutamento.backend.vaga.repository.VagaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Candidatura (RF07/RF08) e painel do RH: inscritos por vaga e etapa do processo (RF12/RF13). */
@Service
@Transactional(readOnly = true)
public class CandidaturaService {

    /** A entrevista e gravada em UTC e mostrada no horario de Brasilia. */
    private static final DateTimeFormatter HORARIO_ENTREVISTA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm").withZone(ZoneId.of("America/Sao_Paulo"));

    private final CandidaturaRepository candidaturaRepository;
    private final HistoricoStatusRepository historicoRepository;
    private final VagaRepository vagaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CurriculoRepository curriculoRepository;
    private final CandidaturaMapper candidaturaMapper;
    private final NotificacaoService notificacaoService;

    public CandidaturaService(CandidaturaRepository candidaturaRepository, HistoricoStatusRepository historicoRepository,
                              VagaRepository vagaRepository, UsuarioRepository usuarioRepository,
                              CurriculoRepository curriculoRepository, CandidaturaMapper candidaturaMapper,
                              NotificacaoService notificacaoService) {
        this.candidaturaRepository = candidaturaRepository;
        this.historicoRepository = historicoRepository;
        this.vagaRepository = vagaRepository;
        this.usuarioRepository = usuarioRepository;
        this.curriculoRepository = curriculoRepository;
        this.candidaturaMapper = candidaturaMapper;
        this.notificacaoService = notificacaoService;
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
        notificacaoService.notificar(vaga.getRh(), "Nova candidatura",
            candidato.getNome() + " se candidatou à vaga " + vaga.getTitulo() + ".");
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
        Candidatura candidatura = obterCandidatura(id);
        verificarResponsavel(candidatura.getVaga(), logado);

        if (mudarEtapa(candidatura, request.status(), request.observacao(), logado)) {
            notificacaoService.notificar(candidatura.getCandidato(), "Sua candidatura mudou de etapa",
                "Vaga " + candidatura.getVaga().getTitulo() + ": sua candidatura agora está em \""
                    + request.status().getRotulo() + "\".");
        }
        return candidaturaMapper.toResponse(candidatura);
    }

    /** Confirma a entrevista: grava a data e hora, leva a candidatura para a etapa entrevista e avisa o candidato. */
    @Transactional
    public CandidaturaResponse agendarEntrevista(Long id, EntrevistaRequest request, UsuarioLogado logado) {
        Candidatura candidatura = obterCandidatura(id);
        verificarResponsavel(candidatura.getVaga(), logado);

        String horario = HORARIO_ENTREVISTA.format(request.dataHora());
        candidatura.agendarEntrevista(request.dataHora());
        mudarEtapa(candidatura, Candidatura.Status.entrevista, "Entrevista agendada para " + horario + ".", logado);
        notificacaoService.notificar(candidatura.getCandidato(), "Entrevista agendada",
            "Vaga " + candidatura.getVaga().getTitulo() + ": sua entrevista foi marcada para " + horario
                + " (horário de Brasília).");
        return candidaturaMapper.toResponse(candidatura);
    }

    /** Muda a etapa e registra no historico. Devolve false quando a etapa ja era a mesma (nada a registrar). */
    private boolean mudarEtapa(Candidatura candidatura, Candidatura.Status novo, String observacao, UsuarioLogado logado) {
        Candidatura.Status anterior = candidatura.getStatus();
        if (anterior == novo) {
            return false;
        }
        candidatura.alterarStatus(novo);
        historicoRepository.save(new HistoricoStatus(candidatura, anterior, novo,
            usuarioRepository.getReferenceById(logado.id()), observacao));
        return true;
    }

    private Candidatura obterCandidatura(Long id) {
        return candidaturaRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Candidatura " + id + " não encontrada."));
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
