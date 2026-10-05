package com.rh.recrutamento.backend.candidatura.service;

import com.rh.recrutamento.backend.analise.dto.response.AnaliseResponse;
import com.rh.recrutamento.backend.analise.service.AnaliseService;
import com.rh.recrutamento.backend.auth.dto.UsuarioLogado;
import com.rh.recrutamento.backend.candidatura.dto.request.CandidaturaRequest;
import com.rh.recrutamento.backend.candidatura.dto.request.EntrevistaRequest;
import com.rh.recrutamento.backend.candidatura.dto.request.StatusCandidaturaRequest;
import com.rh.recrutamento.backend.candidatura.dto.response.CandidaturaResponse;
import com.rh.recrutamento.backend.candidatura.dto.response.CandidaturaRhResponse;
import com.rh.recrutamento.backend.candidatura.entity.Candidatura;
import com.rh.recrutamento.backend.candidatura.entity.HistoricoStatus;
import com.rh.recrutamento.backend.candidatura.exception.CandidaturaNaoPermitidaException;
import com.rh.recrutamento.backend.candidatura.mapper.CandidaturaMapper;
import com.rh.recrutamento.backend.candidatura.repository.CandidaturaRepository;
import com.rh.recrutamento.backend.candidatura.repository.HistoricoStatusRepository;
import com.rh.recrutamento.backend.comum.exception.AcessoNegadoException;
import com.rh.recrutamento.backend.comum.exception.RecursoNaoEncontradoException;
import com.rh.recrutamento.backend.curriculo.repository.CurriculoRepository;
import com.rh.recrutamento.backend.documento.service.DocumentoService;
import com.rh.recrutamento.backend.funcionario.dto.response.FuncionarioResponse;
import com.rh.recrutamento.backend.funcionario.entity.Funcionario;
import com.rh.recrutamento.backend.funcionario.mapper.FuncionarioMapper;
import com.rh.recrutamento.backend.funcionario.repository.FuncionarioRepository;
import com.rh.recrutamento.backend.notificacao.entity.Notificacao;
import com.rh.recrutamento.backend.notificacao.service.NotificacaoService;
import com.rh.recrutamento.backend.usuario.entity.Usuario;
import com.rh.recrutamento.backend.usuario.repository.UsuarioRepository;
import com.rh.recrutamento.backend.vaga.entity.Vaga;
import com.rh.recrutamento.backend.vaga.repository.VagaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
    private final FuncionarioRepository funcionarioRepository;
    private final FuncionarioMapper funcionarioMapper;
    private final DocumentoService documentoService;
    private final AnaliseService analiseService;

    public CandidaturaService(CandidaturaRepository candidaturaRepository, HistoricoStatusRepository historicoRepository,
                              VagaRepository vagaRepository, UsuarioRepository usuarioRepository,
                              CurriculoRepository curriculoRepository, CandidaturaMapper candidaturaMapper,
                              NotificacaoService notificacaoService, FuncionarioRepository funcionarioRepository,
                              FuncionarioMapper funcionarioMapper, DocumentoService documentoService,
                              AnaliseService analiseService) {
        this.candidaturaRepository = candidaturaRepository;
        this.historicoRepository = historicoRepository;
        this.vagaRepository = vagaRepository;
        this.usuarioRepository = usuarioRepository;
        this.curriculoRepository = curriculoRepository;
        this.candidaturaMapper = candidaturaMapper;
        this.notificacaoService = notificacaoService;
        this.funcionarioRepository = funcionarioRepository;
        this.funcionarioMapper = funcionarioMapper;
        this.documentoService = documentoService;
        this.analiseService = analiseService;
    }

    @Transactional
    public CandidaturaResponse candidatar(CandidaturaRequest request, UsuarioLogado logado) {
        Vaga vaga = obterVaga(request.vagaId());
        if (vaga.getStatus() != Vaga.Status.aberta) {
            throw new CandidaturaNaoPermitidaException(
                "Esta vaga não está aberta para candidaturas no momento. Veja as outras vagas disponíveis no portal.");
        }
        if (!curriculoRepository.existsByUsuario_Id(logado.id())) {
            throw new CandidaturaNaoPermitidaException(
                "Para se candidatar, cadastre primeiro o seu currículo. Ele fica salvo para as próximas vagas.");
        }
        if (candidaturaRepository.existsByCandidato_IdAndVaga_Id(logado.id(), vaga.getId())) {
            throw new CandidaturaNaoPermitidaException(
                "Você já se candidatou a esta vaga. Acompanhe o andamento em Minhas candidaturas.");
        }

        Usuario candidato = usuarioRepository.getReferenceById(logado.id());
        Candidatura candidatura = candidaturaRepository.save(new Candidatura(candidato, vaga));
        historicoRepository.save(new HistoricoStatus(
            candidatura, null, Candidatura.Status.inscrito, candidato, "Candidatura registrada pelo portal."));
        notificacaoService.notificar(vaga.getRh(), Notificacao.Tipo.candidatura, candidatura.getId(), "Nova candidatura",
            candidato.getNome() + " se candidatou à vaga " + vaga.getTitulo() + ".");
        analiseService.solicitar(candidatura); // triagem por IA: roda depois do commit e nunca derruba a candidatura
        return candidaturaMapper.toResponse(candidatura);
    }

    /** RN06: o candidato ve so as proprias candidaturas. */
    public List<CandidaturaResponse> listarMinhas(UsuarioLogado logado) {
        return candidaturaRepository.findByCandidato_IdOrderByDataCandidaturaDesc(logado.id()).stream()
            .map(candidaturaMapper::toResponse)
            .toList();
    }

    /**
     * Quem ja foi contratado deixa a lista de candidatos e passa a aparecer em Funcionarios.
     * E o unico ponto que entrega a triagem por IA, e so ao RH da vaga (ou ao administrador).
     * Ordem: maior aderencia primeiro; sem aderencia (pendente, falha ou sem analise) por ultimo;
     * o empate segue a data da candidatura.
     */
    public List<CandidaturaRhResponse> listarPorVaga(Long vagaId, UsuarioLogado logado) {
        verificarResponsavel(obterVaga(vagaId), logado);
        List<Candidatura> candidaturas = candidaturaRepository.findByVaga_IdOrderByDataCandidaturaAsc(vagaId).stream()
            .filter(c -> c.getStatus() != Candidatura.Status.contratado)
            .toList();
        Map<Long, AnaliseResponse> analises =
            analiseService.porCandidatura(candidaturas.stream().map(Candidatura::getId).toList());
        return candidaturas.stream()
            .map(c -> CandidaturaRhResponse.de(candidaturaMapper.toResponse(c), analises.get(c.getId())))
            .sorted(Comparator.comparingInt(CandidaturaService::aderenciaParaOrdenar).reversed())
            .toList();
    }

    @Transactional
    public CandidaturaResponse alterarStatus(Long id, StatusCandidaturaRequest request, UsuarioLogado logado) {
        Candidatura candidatura = obterCandidatura(id);
        verificarResponsavel(candidatura.getVaga(), logado);
        exigirNaoContratada(candidatura);
        if (request.status() == Candidatura.Status.contratado) {
            throw new CandidaturaNaoPermitidaException(
                "Para contratar, use a ação Contratar: ela confere se o candidato está aprovado e com todos os documentos aprovados.");
        }

        if (mudarEtapa(candidatura, request.status(), request.observacao(), logado)) {
            notificacaoService.notificar(candidatura.getCandidato(), Notificacao.Tipo.candidatura, candidatura.getId(),
                "Sua candidatura mudou de etapa", mensagemDaEtapa(candidatura, request.status()));
        }
        return candidaturaMapper.toResponse(candidatura);
    }

    /** Confirma a entrevista: grava a data e hora, leva a candidatura para a etapa entrevista e avisa o candidato. */
    @Transactional
    public CandidaturaResponse agendarEntrevista(Long id, EntrevistaRequest request, UsuarioLogado logado) {
        Candidatura candidatura = obterCandidatura(id);
        verificarResponsavel(candidatura.getVaga(), logado);
        exigirNaoContratada(candidatura);

        String horario = HORARIO_ENTREVISTA.format(request.dataHora());
        candidatura.agendarEntrevista(request.dataHora());
        mudarEtapa(candidatura, Candidatura.Status.entrevista, "Entrevista agendada para " + horario + ".", logado);
        notificacaoService.notificar(candidatura.getCandidato(), Notificacao.Tipo.candidatura, candidatura.getId(),
            "Entrevista agendada",
            "Vaga " + candidatura.getVaga().getTitulo() + ": sua entrevista foi marcada para " + horario
                + " (horário de Brasília).");
        return candidaturaMapper.toResponse(candidatura);
    }

    /** O candidato confirma presenca na entrevista marcada: so o dono da candidatura, e so se ha entrevista marcada. */
    @Transactional
    public CandidaturaResponse confirmarPresenca(Long id, UsuarioLogado logado) {
        Candidatura candidatura = obterCandidatura(id);
        if (!candidatura.getCandidato().getId().equals(logado.id())) {
            throw new AcessoNegadoException("Você só pode confirmar presença nas suas próprias entrevistas.");
        }
        if (candidatura.getStatus() != Candidatura.Status.entrevista || candidatura.getEntrevistaEm() == null) {
            throw new CandidaturaNaoPermitidaException(
                "Não há entrevista marcada para esta candidatura. Assim que o RH marcar, você será avisado.");
        }
        if (candidatura.getPresenca() != Candidatura.Presenca.confirmado) {
            candidatura.confirmarPresenca();
        }
        return candidaturaMapper.toResponse(candidatura);
    }

    /**
     * Agenda do RH: entrevistas marcadas (candidatura ainda na etapa entrevista) das suas vagas, ou de todas
     * para o administrador, da mais proxima para a mais distante. "presenca" filtra confirmadas ou pendentes.
     */
    public List<CandidaturaResponse> agenda(Candidatura.Presenca presenca, UsuarioLogado logado) {
        List<Candidatura> entrevistas = logado.ehAdministrador()
            ? candidaturaRepository.findByStatusAndEntrevistaEmIsNotNullOrderByEntrevistaEmAsc(Candidatura.Status.entrevista)
            : candidaturaRepository.findByStatusAndEntrevistaEmIsNotNullAndVaga_Rh_IdOrderByEntrevistaEmAsc(
                Candidatura.Status.entrevista, logado.id());
        return entrevistas.stream()
            .filter(c -> presenca == null || c.getPresenca() == presenca)
            .map(candidaturaMapper::toResponse)
            .toList();
    }

    /**
     * Contrata: so candidatura aprovada e com todos os documentos obrigatorios aprovados. Na mesma
     * transacao a etapa vira contratado (com historico), nasce o funcionario ativo e o candidato e avisado;
     * se qualquer passo falhar, nada fica gravado.
     */
    @Transactional
    public FuncionarioResponse contratar(Long id, UsuarioLogado logado) {
        Candidatura candidatura = obterCandidatura(id);
        verificarResponsavel(candidatura.getVaga(), logado);
        if (candidatura.getStatus() != Candidatura.Status.aprovado) {
            throw new CandidaturaNaoPermitidaException("Só é possível contratar candidatos com a candidatura aprovada.");
        }
        List<String> faltando = documentoService.obrigatoriosNaoAprovados(id);
        if (!faltando.isEmpty()) {
            throw new CandidaturaNaoPermitidaException(
                "Ainda faltam documentos aprovados para contratar: " + String.join(", ", faltando) + ".");
        }

        mudarEtapa(candidatura, Candidatura.Status.contratado, "Contratação registrada pelo RH.", logado);
        Funcionario funcionario = funcionarioRepository.save(new Funcionario(candidatura));
        notificacaoService.notificar(candidatura.getCandidato(), Notificacao.Tipo.candidatura, candidatura.getId(),
            "Contratação confirmada",
            "Bem-vindo(a) à equipe! Você foi contratado(a) para a vaga " + candidatura.getVaga().getTitulo() + ".");
        return funcionarioMapper.toResponse(funcionario);
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

    /** Texto ao candidato para a nova etapa. Reprovacao e cancelamento levam uma mensagem acolhedora e o proximo passo. */
    private String mensagemDaEtapa(Candidatura candidatura, Candidatura.Status novo) {
        String vaga = candidatura.getVaga().getTitulo();
        return switch (novo) {
            case reprovado -> "Agradecemos o seu interesse e o tempo dedicado à vaga " + vaga
                + ". Desta vez seguiremos com outro perfil, mas o seu currículo continua no portal "
                + "e você pode se candidatar às outras vagas abertas.";
            case cancelado -> "A sua candidatura à vaga " + vaga + " foi encerrada. "
                + "Quando quiser, conheça as outras vagas abertas e candidate-se.";
            default -> "Vaga " + vaga + ": sua candidatura agora está em \"" + novo.getRotulo() + "\".";
        };
    }

    /** Sem aderencia vale -1, para ficar depois de quem tem 0%. A ordenacao e estavel: o empate segue a data. */
    private static int aderenciaParaOrdenar(CandidaturaRhResponse c) {
        return c.analise() == null || c.analise().aderencia() == null ? -1 : c.analise().aderencia();
    }

    /** Depois de contratada, a candidatura so sai desse estado inativando o funcionario. */
    private void exigirNaoContratada(Candidatura candidatura) {
        if (candidatura.getStatus() == Candidatura.Status.contratado) {
            throw new CandidaturaNaoPermitidaException("Este candidato já foi contratado e agora consta em Funcionários.");
        }
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
