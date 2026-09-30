package com.rh.recrutamento.backend.service;

import com.rh.recrutamento.backend.dto.auth.UsuarioLogado;
import com.rh.recrutamento.backend.dto.curriculo.request.CurriculoRequest;
import com.rh.recrutamento.backend.dto.curriculo.request.CurriculoUpdateRequest;
import com.rh.recrutamento.backend.dto.curriculo.request.ExperienciaRequest;
import com.rh.recrutamento.backend.dto.curriculo.request.FormacaoRequest;
import com.rh.recrutamento.backend.dto.curriculo.response.CurriculoResponse;
import com.rh.recrutamento.backend.entity.Curriculo;
import com.rh.recrutamento.backend.entity.CurriculoArquivo;
import com.rh.recrutamento.backend.entity.CurriculoExperiencia;
import com.rh.recrutamento.backend.entity.CurriculoFormacao;
import com.rh.recrutamento.backend.entity.Usuario;
import com.rh.recrutamento.backend.exception.AcessoNegadoException;
import com.rh.recrutamento.backend.exception.ArquivoInvalidoException;
import com.rh.recrutamento.backend.exception.CandidatoInvalidoException;
import com.rh.recrutamento.backend.exception.CurriculoJaExisteException;
import com.rh.recrutamento.backend.exception.RecursoNaoEncontradoException;
import com.rh.recrutamento.backend.mapper.CurriculoMapperImpl;
import com.rh.recrutamento.backend.repository.CandidaturaRepository;
import com.rh.recrutamento.backend.repository.CurriculoRepository;
import com.rh.recrutamento.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurriculoServiceTest {

    @Mock
    private CurriculoRepository curriculoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ArquivoStorage arquivoStorage;

    @Mock
    private CandidaturaRepository candidaturaRepository;

    private CurriculoService curriculoService;

    @BeforeEach
    void montarService() {
        curriculoService = new CurriculoService(
            curriculoRepository, usuarioRepository, new CurriculoMapperImpl(), arquivoStorage, candidaturaRepository);
    }

    @Test
    void criarDeveSalvarDadosPessoaisContatoEListas() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(candidato(1L)));
        when(curriculoRepository.existsByUsuario_Id(1L)).thenReturn(false);
        when(curriculoRepository.save(any(Curriculo.class))).thenAnswer(inv -> inv.getArgument(0));

        CurriculoResponse resposta = curriculoService.criar(requestCompleto(), candidatoLogado(1L));

        ArgumentCaptor<Curriculo> captor = ArgumentCaptor.forClass(Curriculo.class);
        verify(curriculoRepository).save(captor.capture());
        Curriculo salvo = captor.getValue();
        assertThat(salvo.getCidade()).isEqualTo("Campinas");
        assertThat(salvo.getUf()).isEqualTo("SP");
        assertThat(salvo.getNumeroContato()).isEqualTo("19999990000");
        assertThat(salvo.getPerfilLinkedin()).isEqualTo("https://linkedin.com/in/ana");
        assertThat(salvo.getSexo()).isEqualTo(Curriculo.Sexo.feminino);
        assertThat(salvo.getCertificacoes()).isEqualTo("AWS Cloud Practitioner");
        assertThat(salvo.getFormacoes()).hasSize(1);
        assertThat(salvo.getFormacoes().getFirst().getCurso()).isEqualTo("ADS");
        assertThat(salvo.getFormacoes().getFirst().getCurriculo()).isSameAs(salvo);
        assertThat(salvo.getExperiencias()).hasSize(1);
        assertThat(salvo.getExperiencias().getFirst().getEmpresa()).isEqualTo("Acme");
        assertThat(resposta.usuarioId()).isEqualTo(1L);
    }

    @Test
    void criarDeveDescartarDataDemissaoQuandoTrabalhoAtual() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(candidato(1L)));
        when(curriculoRepository.existsByUsuario_Id(1L)).thenReturn(false);
        when(curriculoRepository.save(any(Curriculo.class))).thenAnswer(inv -> inv.getArgument(0));

        ExperienciaRequest atual = new ExperienciaRequest(
            "Dev", "Acme", LocalDate.of(2023, 1, 10), LocalDate.of(2024, 5, 30), true, "Backend");
        curriculoService.criar(comExperiencias(List.of(atual)), candidatoLogado(1L));

        ArgumentCaptor<Curriculo> captor = ArgumentCaptor.forClass(Curriculo.class);
        verify(curriculoRepository).save(captor.capture());
        CurriculoExperiencia gravada = captor.getValue().getExperiencias().getFirst();
        assertThat(gravada.isTrabalhoAtual()).isTrue();
        assertThat(gravada.getDataDemissao()).isNull();
    }

    @Test
    void criarDeveManterDataDemissaoQuandoNaoEhTrabalhoAtual() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(candidato(1L)));
        when(curriculoRepository.existsByUsuario_Id(1L)).thenReturn(false);
        when(curriculoRepository.save(any(Curriculo.class))).thenAnswer(inv -> inv.getArgument(0));

        ExperienciaRequest anterior = new ExperienciaRequest(
            "Dev", "Acme", LocalDate.of(2023, 1, 10), LocalDate.of(2024, 5, 30), false, "Backend");
        curriculoService.criar(comExperiencias(List.of(anterior)), candidatoLogado(1L));

        ArgumentCaptor<Curriculo> captor = ArgumentCaptor.forClass(Curriculo.class);
        verify(curriculoRepository).save(captor.capture());
        assertThat(captor.getValue().getExperiencias().getFirst().getDataDemissao())
            .isEqualTo(LocalDate.of(2024, 5, 30));
    }

    @Test
    void criarDeveRecusarQuandoUsuarioNaoEhCandidato() {
        Usuario rh = new Usuario("Rita", "rita@rh.com", "hash", Usuario.Perfil.rh, Usuario.Status.ativo);
        ReflectionTestUtils.setField(rh, "id", 9L);
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(rh));

        assertThatThrownBy(() -> curriculoService.criar(requestCompleto(), candidatoLogado(9L)))
            .isInstanceOf(CandidatoInvalidoException.class);
        verify(curriculoRepository, never()).save(any());
    }

    @Test
    void criarDeveRecusarQuandoUsuarioNaoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> curriculoService.criar(requestCompleto(), candidatoLogado(99L)))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void criarDeveRecusarQuandoCandidatoJaPossuiCurriculo() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(candidato(1L)));
        when(curriculoRepository.existsByUsuario_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> curriculoService.criar(requestCompleto(), candidatoLogado(1L)))
            .isInstanceOf(CurriculoJaExisteException.class);
        verify(curriculoRepository, never()).save(any());
    }

    @Test
    void buscarPorIdDeveCalcularIdadeAPartirDaDataDeNascimento() {
        LocalDate nascimento = LocalDate.now().minusYears(27).minusDays(3);
        Curriculo curriculo = curriculoDe(5L, candidato(1L));
        curriculo.atualizarDados(nascimento, Curriculo.Sexo.outro, "Campinas", "SP",
            null, null, null, null, "Resumo");
        when(curriculoRepository.findById(5L)).thenReturn(Optional.of(curriculo));

        CurriculoResponse resposta = curriculoService.buscarPorId(5L, candidatoLogado(1L));

        assertThat(resposta.dataNascimento()).isEqualTo(nascimento);
        assertThat(resposta.idade()).isEqualTo(27);
    }

    @Test
    void buscarPorIdDeveDevolverIdadeNulaSemDataDeNascimento() {
        when(curriculoRepository.findById(5L)).thenReturn(Optional.of(curriculoDe(5L, candidato(1L))));

        assertThat(curriculoService.buscarPorId(5L, candidatoLogado(1L)).idade()).isNull();
    }

    @Test
    void buscarPorIdDeveFalharQuandoNaoExiste() {
        when(curriculoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> curriculoService.buscarPorId(99L, candidatoLogado(1L)))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void buscarPorUsuarioDeveRetornarCurriculo() {
        when(curriculoRepository.findByUsuario_Id(1L)).thenReturn(Optional.of(curriculoDe(5L, candidato(1L))));

        CurriculoResponse resposta = curriculoService.buscarPorUsuario(1L, candidatoLogado(1L));

        assertThat(resposta.id()).isEqualTo(5L);
        assertThat(resposta.usuarioId()).isEqualTo(1L);
    }

    @Test
    void buscarPorUsuarioDeveFalharQuandoNaoExiste() {
        when(curriculoRepository.findByUsuario_Id(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> curriculoService.buscarPorUsuario(77L, candidatoLogado(77L)))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void atualizarDeveSubstituirListasPorCompleto() {
        Curriculo curriculo = curriculoDe(5L, candidato(1L));
        curriculo.adicionarFormacao(new CurriculoFormacao(
            "Curso antigo", "Escola antiga", LocalDate.of(2015, 1, 1), LocalDate.of(2018, 12, 1)));
        when(curriculoRepository.findById(5L)).thenReturn(Optional.of(curriculo));
        when(curriculoRepository.save(any(Curriculo.class))).thenAnswer(inv -> inv.getArgument(0));

        CurriculoUpdateRequest request = new CurriculoUpdateRequest(
            LocalDate.of(1999, 3, 20), Curriculo.Sexo.masculino, "Sao Paulo", "SP",
            "11988887777", "https://linkedin.com/in/joao", "Java, SQL", "Scrum Foundation", "Novo resumo",
            List.of(new FormacaoRequest("Ciencia da Computacao", "USP", LocalDate.of(2019, 2, 1), null)),
            List.of());

        CurriculoResponse resposta = curriculoService.atualizar(5L, request, candidatoLogado(1L));

        assertThat(curriculo.getFormacoes()).hasSize(1);
        assertThat(curriculo.getFormacoes().getFirst().getCurso()).isEqualTo("Ciencia da Computacao");
        assertThat(curriculo.getFormacoes().getFirst().getDataTermino()).isNull();
        assertThat(curriculo.getExperiencias()).isEmpty();
        assertThat(resposta.cidade()).isEqualTo("Sao Paulo");
        assertThat(resposta.certificacoes()).isEqualTo("Scrum Foundation");
    }

    @Test
    void atualizarDeveFalharQuandoNaoExiste() {
        when(curriculoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> curriculoService.atualizar(99L, new CurriculoUpdateRequest(
            null, null, null, null, null, null, null, null, null, List.of(), List.of()), candidatoLogado(1L)))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void anexarArquivoDeveGuardarMetadadosDoPdf() {
        Curriculo curriculo = curriculoDe(5L, candidato(1L));
        when(curriculoRepository.findById(5L)).thenReturn(Optional.of(curriculo));
        when(curriculoRepository.save(any(Curriculo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(arquivoStorage.salvar(eq("curriculo"), any(), eq("pdf"))).thenReturn("gerado.pdf");

        MockMultipartFile pdf = new MockMultipartFile(
            "arquivo", "curriculo-ana.pdf", "application/pdf", "conteudo".getBytes());
        CurriculoResponse resposta = curriculoService.anexarArquivo(5L, pdf, candidatoLogado(1L));

        CurriculoArquivo arquivo = curriculo.getArquivo();
        assertThat(arquivo.getNomeOriginal()).isEqualTo("curriculo-ana.pdf");
        assertThat(arquivo.getNomeArmazenado()).isEqualTo("gerado.pdf");
        assertThat(arquivo.getContentType()).isEqualTo("application/pdf");
        assertThat(arquivo.getTamanhoBytes()).isEqualTo(8L);
        assertThat(arquivo.getCurriculo()).isSameAs(curriculo);
        assertThat(resposta.arquivo().nomeOriginal()).isEqualTo("curriculo-ana.pdf");
        verify(arquivoStorage, never()).remover(any(), any());
    }

    @Test
    void anexarArquivoDeveApagarOAnteriorAoSubstituir() {
        Curriculo curriculo = curriculoDe(5L, candidato(1L));
        curriculo.definirArquivo(new CurriculoArquivo("antigo.pdf", "antigo-gerado.pdf", "application/pdf", 10L));
        when(curriculoRepository.findById(5L)).thenReturn(Optional.of(curriculo));
        when(curriculoRepository.save(any(Curriculo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(arquivoStorage.salvar(eq("curriculo"), any(), eq("pdf"))).thenReturn("novo-gerado.pdf");

        curriculoService.anexarArquivo(5L, new MockMultipartFile(
            "arquivo", "novo.pdf", "application/pdf", "novo".getBytes()), candidatoLogado(1L));

        assertThat(curriculo.getArquivo().getNomeArmazenado()).isEqualTo("novo-gerado.pdf");
        verify(arquivoStorage).remover("curriculo", "antigo-gerado.pdf");
    }

    @Test
    void anexarArquivoQueNaoEhPdfDeveSerRecusadoSemGravar() {
        when(curriculoRepository.findById(5L)).thenReturn(Optional.of(curriculoDe(5L, candidato(1L))));

        assertThatThrownBy(() -> curriculoService.anexarArquivo(5L, new MockMultipartFile(
            "arquivo", "foto.png", "image/png", "png".getBytes()), candidatoLogado(1L)))
            .isInstanceOf(ArquivoInvalidoException.class)
            .hasMessage("Somente arquivos PDF sao aceitos.");
        verify(arquivoStorage, never()).salvar(any(), any(), any());
    }

    @Test
    void baixarArquivoDeveFalharQuandoCurriculoNaoTemAnexo() {
        when(curriculoRepository.findById(5L)).thenReturn(Optional.of(curriculoDe(5L, candidato(1L))));

        assertThatThrownBy(() -> curriculoService.baixarArquivo(5L, candidatoLogado(1L)))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessageContaining("nao possui arquivo");
    }

    @Test
    void baixarArquivoDeveDevolverConteudoDoDisco() {
        Curriculo curriculo = curriculoDe(5L, candidato(1L));
        curriculo.definirArquivo(new CurriculoArquivo("ana.pdf", "gerado.pdf", "application/pdf", 4L));
        when(curriculoRepository.findById(5L)).thenReturn(Optional.of(curriculo));
        when(arquivoStorage.ler("curriculo", "gerado.pdf")).thenReturn("pdf!".getBytes());

        CurriculoService.ArquivoBaixado baixado = curriculoService.baixarArquivo(5L, candidatoLogado(1L));

        assertThat(baixado.nomeOriginal()).isEqualTo("ana.pdf");
        assertThat(baixado.contentType()).isEqualTo("application/pdf");
        assertThat(baixado.conteudo()).isEqualTo("pdf!".getBytes());
    }

    @Test
    void atualizarCurriculoDeOutroCandidatoDeveSerNegado() {
        when(curriculoRepository.findById(5L)).thenReturn(Optional.of(curriculoDe(5L, candidato(1L))));

        assertThatThrownBy(() -> curriculoService.atualizar(5L, new CurriculoUpdateRequest(
            null, null, null, null, null, null, null, null, null, List.of(), List.of()), candidatoLogado(2L)))
            .isInstanceOf(AcessoNegadoException.class);
        verify(curriculoRepository, never()).save(any());
    }

    @Test
    void anexarArquivoEmCurriculoDeOutroCandidatoDeveSerNegado() {
        when(curriculoRepository.findById(5L)).thenReturn(Optional.of(curriculoDe(5L, candidato(1L))));

        assertThatThrownBy(() -> curriculoService.anexarArquivo(5L, new MockMultipartFile(
            "arquivo", "x.pdf", "application/pdf", "x".getBytes()), candidatoLogado(2L)))
            .isInstanceOf(AcessoNegadoException.class);
        verify(arquivoStorage, never()).salvar(any(), any(), any());
    }

    @Test
    void buscarCurriculoDeOutroCandidatoDeveSerNegadoSemConsultarOBanco() {
        assertThatThrownBy(() -> curriculoService.buscarPorUsuario(1L, candidatoLogado(2L)))
            .isInstanceOf(AcessoNegadoException.class);
        verify(curriculoRepository, never()).findByUsuario_Id(any());
    }

    @Test
    void administradorPodeLerQualquerCurriculo() {
        when(curriculoRepository.findById(5L)).thenReturn(Optional.of(curriculoDe(5L, candidato(1L))));

        assertThat(curriculoService.buscarPorId(5L, new UsuarioLogado(9L, Usuario.Perfil.administrador)).id())
            .isEqualTo(5L);
    }

    @Test
    void rhLeCurriculoDeQuemSeInscreveuEmVagaDele() {
        when(curriculoRepository.findById(5L)).thenReturn(Optional.of(curriculoDe(5L, candidato(1L))));
        when(candidaturaRepository.existsByCandidato_IdAndVaga_Rh_Id(1L, 7L)).thenReturn(true);

        assertThat(curriculoService.buscarPorId(5L, new UsuarioLogado(7L, Usuario.Perfil.rh)).id()).isEqualTo(5L);
    }

    @Test
    void rhNaoLeCurriculoDeQuemNaoSeInscreveuEmVagaDele() {
        when(candidaturaRepository.existsByCandidato_IdAndVaga_Rh_Id(1L, 8L)).thenReturn(false);

        assertThatThrownBy(() -> curriculoService.buscarPorUsuario(1L, new UsuarioLogado(8L, Usuario.Perfil.rh)))
            .isInstanceOf(AcessoNegadoException.class);
        verify(curriculoRepository, never()).findByUsuario_Id(any());
    }

    private UsuarioLogado candidatoLogado(Long id) {
        return new UsuarioLogado(id, Usuario.Perfil.candidato);
    }

    private Usuario candidato(Long id) {
        Usuario usuario = new Usuario(
            "Ana Souza", "ana@teste.com", "hash", Usuario.Perfil.candidato, Usuario.Status.ativo);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    private Curriculo curriculoDe(Long id, Usuario usuario) {
        Curriculo curriculo = new Curriculo(usuario);
        ReflectionTestUtils.setField(curriculo, "id", id);
        return curriculo;
    }

    private CurriculoRequest requestCompleto() {
        return new CurriculoRequest(
            LocalDate.of(1998, 4, 12), Curriculo.Sexo.feminino, "Campinas", "SP",
            "19999990000", "https://linkedin.com/in/ana", "Java, SQL", "AWS Cloud Practitioner", "Resumo",
            List.of(new FormacaoRequest("ADS", "Fatec", LocalDate.of(2020, 2, 1), LocalDate.of(2023, 12, 15))),
            List.of(new ExperienciaRequest("Dev Junior", "Acme", LocalDate.of(2023, 1, 10),
                LocalDate.of(2024, 5, 30), false, "Manutencao de APIs")));
    }

    private CurriculoRequest comExperiencias(List<ExperienciaRequest> experiencias) {
        return new CurriculoRequest(null, null, null, null, null, null, null, null, null,
            List.of(), experiencias);
    }
}
