package com.rh.recrutamento.backend.candidatura.service;

import com.rh.recrutamento.backend.auth.dto.UsuarioLogado;
import com.rh.recrutamento.backend.candidatura.dto.request.CandidaturaRequest;
import com.rh.recrutamento.backend.candidatura.dto.request.EntrevistaRequest;
import com.rh.recrutamento.backend.candidatura.dto.request.StatusCandidaturaRequest;
import com.rh.recrutamento.backend.candidatura.dto.response.CandidaturaResponse;
import com.rh.recrutamento.backend.candidatura.dto.response.CandidaturaRhResponse;
import com.rh.recrutamento.backend.candidatura.entity.Candidatura;
import com.rh.recrutamento.backend.candidatura.entity.HistoricoStatus;
import com.rh.recrutamento.backend.candidatura.exception.CandidaturaNaoPermitidaException;
import com.rh.recrutamento.backend.candidatura.repository.CandidaturaRepository;
import com.rh.recrutamento.backend.candidatura.repository.HistoricoStatusRepository;
import com.rh.recrutamento.backend.comum.exception.AcessoNegadoException;
import com.rh.recrutamento.backend.curriculo.repository.CurriculoRepository;
import com.rh.recrutamento.backend.analise.service.AnaliseService;
import com.rh.recrutamento.backend.documento.service.DocumentoService;
import com.rh.recrutamento.backend.funcionario.mapper.FuncionarioMapperImpl;
import com.rh.recrutamento.backend.funcionario.repository.FuncionarioRepository;
import com.rh.recrutamento.backend.notificacao.entity.Notificacao;
import com.rh.recrutamento.backend.notificacao.service.NotificacaoService;
import com.rh.recrutamento.backend.usuario.entity.Usuario;
import com.rh.recrutamento.backend.usuario.repository.UsuarioRepository;
import com.rh.recrutamento.backend.vaga.entity.Vaga;
import com.rh.recrutamento.backend.vaga.repository.VagaRepository;
import com.rh.recrutamento.backend.candidatura.mapper.CandidaturaMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidaturaServiceTest {

    private static final UsuarioLogado ANA = new UsuarioLogado(1L, Usuario.Perfil.candidato);
    private static final UsuarioLogado RITA = new UsuarioLogado(7L, Usuario.Perfil.rh);
    private static final UsuarioLogado PAULO = new UsuarioLogado(8L, Usuario.Perfil.rh);
    private static final UsuarioLogado ADMINISTRADOR = new UsuarioLogado(9L, Usuario.Perfil.administrador);

    @Mock
    private CandidaturaRepository candidaturaRepository;

    @Mock
    private HistoricoStatusRepository historicoRepository;

    @Mock
    private VagaRepository vagaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CurriculoRepository curriculoRepository;

    @Mock
    private NotificacaoService notificacaoService;

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private DocumentoService documentoService;

    @Mock
    private AnaliseService analiseService;

    private CandidaturaService candidaturaService;

    @BeforeEach
    void montarService() {
        candidaturaService = new CandidaturaService(candidaturaRepository, historicoRepository, vagaRepository,
            usuarioRepository, curriculoRepository, new CandidaturaMapperImpl(), notificacaoService,
            funcionarioRepository, new FuncionarioMapperImpl(), documentoService, analiseService);
    }

    @Test
    void candidatarDeveCriarInscricaoERegistrarHistorico() {
        Vaga vaga = vaga(10L, Vaga.Status.aberta);
        when(vagaRepository.findById(10L)).thenReturn(Optional.of(vaga));
        when(curriculoRepository.existsByUsuario_Id(1L)).thenReturn(true);
        when(candidaturaRepository.existsByCandidato_IdAndVaga_Id(1L, 10L)).thenReturn(false);
        when(usuarioRepository.getReferenceById(1L)).thenReturn(usuario(1L, Usuario.Perfil.candidato));
        when(candidaturaRepository.save(any(Candidatura.class))).thenAnswer(inv -> inv.getArgument(0));

        CandidaturaResponse resposta = candidaturaService.candidatar(new CandidaturaRequest(10L), ANA);

        assertThat(resposta.status()).isEqualTo("inscrito");
        assertThat(resposta.vagaId()).isEqualTo(10L);
        assertThat(resposta.candidatoId()).isEqualTo(1L);
        ArgumentCaptor<HistoricoStatus> historico = ArgumentCaptor.forClass(HistoricoStatus.class);
        verify(historicoRepository).save(historico.capture());
        assertThat(historico.getValue().getStatusAnterior()).isNull();
        assertThat(historico.getValue().getStatusNovo()).isEqualTo(Candidatura.Status.inscrito);
        // o RH responsavel pela vaga e avisado
        verify(notificacaoService).notificar(eq(vaga.getRh()), eq(Notificacao.Tipo.candidatura), any(),
            eq("Nova candidatura"), eq("Ana se candidatou à vaga Backend Java."));
    }

    @Test
    void candidatarEmVagaNaoAbertaDeveSerRecusado() {
        when(vagaRepository.findById(10L)).thenReturn(Optional.of(vaga(10L, Vaga.Status.encerrada)));

        assertThatThrownBy(() -> candidaturaService.candidatar(new CandidaturaRequest(10L), ANA))
            .isInstanceOf(CandidaturaNaoPermitidaException.class)
            .hasMessageContaining("não está aberta");
        verify(candidaturaRepository, never()).save(any());
    }

    @Test
    void candidatarSemCurriculoDeveSerRecusado() {
        when(vagaRepository.findById(10L)).thenReturn(Optional.of(vaga(10L, Vaga.Status.aberta)));
        when(curriculoRepository.existsByUsuario_Id(1L)).thenReturn(false);

        assertThatThrownBy(() -> candidaturaService.candidatar(new CandidaturaRequest(10L), ANA))
            .isInstanceOf(CandidaturaNaoPermitidaException.class)
            .hasMessageContaining("currículo");
        verify(candidaturaRepository, never()).save(any());
    }

    @Test
    void candidatarDuasVezesNaMesmaVagaDeveSerRecusado() {
        when(vagaRepository.findById(10L)).thenReturn(Optional.of(vaga(10L, Vaga.Status.aberta)));
        when(curriculoRepository.existsByUsuario_Id(1L)).thenReturn(true);
        when(candidaturaRepository.existsByCandidato_IdAndVaga_Id(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> candidaturaService.candidatar(new CandidaturaRequest(10L), ANA))
            .isInstanceOf(CandidaturaNaoPermitidaException.class)
            .hasMessageContaining("já se candidatou");
        verify(candidaturaRepository, never()).save(any());
    }

    @Test
    void listarPorVagaDeOutroRhDeveSerNegado() {
        when(vagaRepository.findById(10L)).thenReturn(Optional.of(vaga(10L, Vaga.Status.aberta)));

        assertThatThrownBy(() -> candidaturaService.listarPorVaga(10L, PAULO))
            .isInstanceOf(AcessoNegadoException.class);
        verify(candidaturaRepository, never()).findByVaga_IdOrderByDataCandidaturaAsc(any());
    }

    @Test
    void listarPorVagaDoProprioRhOuDoAdministradorDeveTrazerInscritos() {
        Vaga vaga = vaga(10L, Vaga.Status.aberta);
        when(vagaRepository.findById(10L)).thenReturn(Optional.of(vaga));
        when(candidaturaRepository.findByVaga_IdOrderByDataCandidaturaAsc(10L))
            .thenReturn(List.of(candidatura(20L, vaga, Candidatura.Status.inscrito)));

        assertThat(candidaturaService.listarPorVaga(10L, RITA))
            .extracting(CandidaturaRhResponse::candidatoNome).containsExactly("Ana");
        assertThat(candidaturaService.listarPorVaga(10L, ADMINISTRADOR)).hasSize(1);
    }

    @Test
    void alterarStatusDeveMudarEtapaERegistrarQuemMudou() {
        Candidatura candidatura = candidatura(20L, vaga(10L, Vaga.Status.aberta), Candidatura.Status.inscrito);
        when(candidaturaRepository.findById(20L)).thenReturn(Optional.of(candidatura));
        when(usuarioRepository.getReferenceById(7L)).thenReturn(usuario(7L, Usuario.Perfil.rh));

        CandidaturaResponse resposta = candidaturaService.alterarStatus(20L,
            new StatusCandidaturaRequest(Candidatura.Status.entrevista, "Entrevista marcada"), RITA);

        assertThat(resposta.status()).isEqualTo("entrevista");
        ArgumentCaptor<HistoricoStatus> historico = ArgumentCaptor.forClass(HistoricoStatus.class);
        verify(historicoRepository).save(historico.capture());
        assertThat(historico.getValue().getStatusAnterior()).isEqualTo(Candidatura.Status.inscrito);
        assertThat(historico.getValue().getStatusNovo()).isEqualTo(Candidatura.Status.entrevista);
        assertThat(historico.getValue().getUsuario().getId()).isEqualTo(7L);
        assertThat(historico.getValue().getObservacao()).isEqualTo("Entrevista marcada");
        verify(notificacaoService).notificar(eq(candidatura.getCandidato()), eq(Notificacao.Tipo.candidatura), eq(20L),
            anyString(), contains("\"Entrevista\""));
    }

    @Test
    void agendarEntrevistaGravaEmUtcMudaAEtapaEAvisaNoHorarioDeBrasilia() {
        Candidatura candidatura = candidatura(20L, vaga(10L, Vaga.Status.aberta), Candidatura.Status.em_triagem);
        when(candidaturaRepository.findById(20L)).thenReturn(Optional.of(candidatura));
        when(usuarioRepository.getReferenceById(7L)).thenReturn(usuario(7L, Usuario.Perfil.rh));
        Instant quando = Instant.parse("2030-10-15T17:30:00Z");

        CandidaturaResponse resposta = candidaturaService.agendarEntrevista(20L, new EntrevistaRequest(quando), RITA);

        assertThat(resposta.status()).isEqualTo("entrevista");
        assertThat(resposta.entrevistaEm()).isEqualTo(quando);
        ArgumentCaptor<HistoricoStatus> historico = ArgumentCaptor.forClass(HistoricoStatus.class);
        verify(historicoRepository).save(historico.capture());
        assertThat(historico.getValue().getStatusNovo()).isEqualTo(Candidatura.Status.entrevista);
        // 17:30 UTC = 14:30 em Brasilia
        verify(notificacaoService).notificar(candidatura.getCandidato(), Notificacao.Tipo.candidatura, 20L,
            "Entrevista agendada",
            "Vaga Backend Java: sua entrevista foi marcada para 15/10/2030 às 14:30 (horário de Brasília).");
    }

    @Test
    void agendarEntrevistaEmVagaDeOutroRhDeveSerNegado() {
        Candidatura candidatura = candidatura(20L, vaga(10L, Vaga.Status.aberta), Candidatura.Status.em_triagem);
        when(candidaturaRepository.findById(20L)).thenReturn(Optional.of(candidatura));

        assertThatThrownBy(() -> candidaturaService.agendarEntrevista(20L,
            new EntrevistaRequest(Instant.parse("2030-10-15T17:30:00Z")), PAULO))
            .isInstanceOf(AcessoNegadoException.class);
        assertThat(candidatura.getEntrevistaEm()).isNull();
        verifyNoInteractions(historicoRepository, notificacaoService);
    }

    @Test
    void alterarParaOMesmoStatusNaoDeveGerarHistorico() {
        Candidatura candidatura = candidatura(20L, vaga(10L, Vaga.Status.aberta), Candidatura.Status.entrevista);
        when(candidaturaRepository.findById(20L)).thenReturn(Optional.of(candidatura));

        candidaturaService.alterarStatus(20L, new StatusCandidaturaRequest(Candidatura.Status.entrevista, null), RITA);

        verifyNoInteractions(historicoRepository, notificacaoService);
    }

    @Test
    void alterarStatusEmVagaDeOutroRhDeveSerNegado() {
        Candidatura candidatura = candidatura(20L, vaga(10L, Vaga.Status.aberta), Candidatura.Status.inscrito);
        when(candidaturaRepository.findById(20L)).thenReturn(Optional.of(candidatura));

        assertThatThrownBy(() -> candidaturaService.alterarStatus(20L,
            new StatusCandidaturaRequest(Candidatura.Status.aprovado, null), PAULO))
            .isInstanceOf(AcessoNegadoException.class);
        assertThat(candidatura.getStatus()).isEqualTo(Candidatura.Status.inscrito);
        verifyNoInteractions(historicoRepository);
    }

    /** Vaga da Rita (id 7). */
    private Vaga vaga(Long id, Vaga.Status status) {
        Vaga vaga = new Vaga(usuario(7L, Usuario.Perfil.rh), "Backend Java", "Descricao", null, null, null, null,
            Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, status, null);
        ReflectionTestUtils.setField(vaga, "id", id);
        return vaga;
    }

    private Candidatura candidatura(Long id, Vaga vaga, Candidatura.Status status) {
        Candidatura candidatura = new Candidatura(usuario(1L, Usuario.Perfil.candidato), vaga);
        candidatura.alterarStatus(status);
        ReflectionTestUtils.setField(candidatura, "id", id);
        return candidatura;
    }

    private Usuario usuario(Long id, Usuario.Perfil perfil) {
        Usuario usuario = new Usuario(perfil == Usuario.Perfil.candidato ? "Ana" : "Rita",
            "u" + id + "@teste.com", "hash", perfil, Usuario.Status.ativo);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }
}
